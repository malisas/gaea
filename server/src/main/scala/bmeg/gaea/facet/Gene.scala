package bmeg.gaea.facet

import bmeg.gaea.titan.Titan
import bmeg.gaea.schema.Variant
import bmeg.gaea.convoy.Ingest
import bmeg.gaea.feature.Feature
import bmeg.gaea.worker.SignatureWorker

import org.http4s._
import org.http4s.server._
import org.http4s.dsl._

import com.thinkaurelius.titan.core.TitanGraph
import gremlin.scala._
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.P._

import com.typesafe.scalalogging._
import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._
import scalaz.stream.text
import scalaz.stream.Process
import scalaz.stream.Process._
import scalaz.stream.Process1
import scalaz.concurrent.Task

object GeneFacet extends LazyLogging {
  val graph = Titan.connect(Titan.configuration(Map[String, String]()))
  val Name = Key[String]("name")
  val Coefficients = Key[String]("coefficients")
  val SampleType = Key[String]("sampleType")

  def puts(line: String): Task[Unit] = Task { println(line) }

  def commit(graph: TitanGraph): Process[Task, Unit] = Process eval_ (Task {
    graph.tx.commit()
  })

  def coefficientsToJson(coefficients: Map[String, Double]) (key: String) (value: String): Json = {
    coefficients.foldLeft(jEmptyArray) { (json, coefficient) =>
      val (feature, level) = coefficient
      val pair = (key, jString(feature)) ->: (value, jNumber(level).getOrElse(jZero)) ->: jEmptyObject
      pair -->>: json
    }
  }

  def propertiesToJson(properties: Map[String, Any]) (key: String) (value: String): Json = {
    properties.foldLeft(jEmptyArray) { (json, property) =>
      val (name, attribute) = property
      val pair = (key, jString(name)) ->: (value, jString(attribute.toString)) ->: jEmptyObject
      pair -->>: json
    }
  }

  def eventMetadata(eventID: String, eventType: String, datatype: String, weights: Map[String, Double]): Json = {
    val weightsJson = coefficientsToJson(weights) ("feature") ("weight")
    ("eventID", jString(eventID)) ->: ("eventType", jString(eventType)) ->: ("datatype", jString(datatype)) ->: ("featureWeights", weightsJson) ->: jEmptyObject
  }

  def signatureToJson(featureNames: List[String]) (vertex: Vertex): Json = {
    val coefficients = SignatureWorker.dehydrateCoefficients(vertex.property(Coefficients).orElse(""))
    val relevant = SignatureWorker.selectKeys[String, Double](coefficients) (featureNames) (0.0)
    val score = relevant.values.foldLeft(0.0) (_ + _)
    val signatureName = vertex.property(Name).orElse("no name")
    val metadata = eventMetadata(signatureName, "drug sensitivity signature", "NUMERIC", relevant)
    ("score", jNumber(score).getOrElse(jZero)) ->: ("signatureMetadata", metadata) ->: jEmptyObject
  }

  def individualEvent(individualVertex: Vertex) (clinicalNames: List[String]): Json = {
    val metadata = eventMetadata(individualVertex.property(Name).orElse(""), "clinical values", "STRING", Map[String, Double]())
    val relevant = SignatureWorker.selectKeys[String, Any](individualVertex.valueMap()) (clinicalNames) ("")
    val clinicalJson = propertiesToJson(relevant) ("sampleID") ("value")
    ("metadata", metadata) ->: ("sampleData", clinicalJson) ->: jEmptyObject
  }

  def clinicalEvent(individualVertexes: Seq[Vertex]) (clinicalName: String): Json = {
    val metadata = eventMetadata(clinicalName, "clinical", "STRING", Map[String, Double]())
    val properties = individualVertexes.map(vertex => (vertex.property("name").orElse(""), vertex.property(clinicalName).orElse(""))).toMap
    val json = propertiesToJson(properties) ("sampleID") ("value")
    ("metadata", metadata) ->: ("sampleData", json) ->: jEmptyObject
  }

  def expressionEvent(expressions: Seq[Tuple3[String, Vertex, Map[String, Double]]]) (gene: String): Json = {
    val individuals = expressions.map(_._1)
    val coefficients = expressions.map(_._3)
    val metadata = eventMetadata(gene, "mrna_expression", "NUMERIC", Map[String, Double]())
    val expression = coefficients.map(_.get(gene).getOrElse(0.0))
    val properties = individuals.zip(expression).toMap
    val json = coefficientsToJson(properties) ("sampleID") ("value")
    ("metadata", metadata) ->: ("sampleData", json) ->: jEmptyObject
  }

  def levelEvent(levels: Map[String, Double]) (signature: String): Json = {
    val metadata = eventMetadata(signature, "drug sensitivity score", "NUMERIC", Map[String, Double]())
    val json = coefficientsToJson(levels) ("sampleID") ("value")
    ("metadata", metadata) ->: ("sampleData", json) ->: jEmptyObject
  }

  def takeHighest(n: Int) (signature: Vertex): List[String] = {
    SignatureWorker.dehydrateCoefficients(signature.property("coefficients").orElse("")).toList.sortWith(_._2 > _._2).take(n).map(_._1)
  }

  val service = HttpService {
    case GET -> Root / "gaea" / "hello" / name =>
      Ok(jSingleObject("message", jString(s"Hello, ${name}")))

    case GET -> Root / "gaea" / "gene" / name =>
      val synonym = Feature.findSynonym(graph) (name).getOrElse {
        "no synonym found"
      }
      Ok(jSingleObject(name, jString(synonym)))

    case request @ POST -> Root / "gaea" / "signature" / "gene" =>
      request.as[Json].flatMap { json => 
        val geneNames = json.as[List[String]].getOr(List[String]())
        val featureVertexes = geneNames.map(Feature.findSynonymVertex(graph) (_)).flatten
        val featureNames = featureVertexes.map(_.property(Name).orElse(""))
        val signatureVertexes = featureVertexes.flatMap(_.in("hasCoefficient").toList).toSet
        val signatureJson = signatureVertexes.map(signatureToJson(featureNames))
        Ok(signatureJson.asJson)
      }

    case request @ POST -> Root / "gaea" / "signature" / "sample" =>
      request.as[Json].flatMap { json =>
        val metadata = json.as[Map[String, List[Map[String, String]]]].getOr(Map[String, List[Map[String, String]]]())
        val signatureMetadata = metadata("signatureMetadata")
        val expressionMetadata = metadata("expressionMetadata")
        val clinicalEventMetadata = metadata("clinicalEventMetadata")

        val signatureNames = signatureMetadata.map(_("eventID"))
        val expressionNames = expressionMetadata.map(_("eventID"))
        val clinicalNames = clinicalEventMetadata.map(_("eventID"))

        val signatureStep = StepLabel[Vertex]()
        val expressionStep = StepLabel[Vertex]()
        val individualStep = StepLabel[Vertex]()
        val levelStep = StepLabel[Edge]()

        val highestQuery = graph.V.hasLabel("linearSignature")
          .has(Name, within(signatureNames:_*)).as(signatureStep)
          .outE("appliesTo").orderBy("level", Order.decr).limit(100)
          .inV.as(expressionStep)
          .out("expressionFor")
          .has(SampleType, "tumor")
          .out("sampleOf").as(individualStep)
          .select((signatureStep, expressionStep, individualStep)).toSet

        val lowestQuery = graph.V.hasLabel("linearSignature")
          .has(Name, within(signatureNames:_*)).as(signatureStep)
          .outE("appliesTo").orderBy("level", Order.incr).limit(100)
          .inV.as(expressionStep)
          .out("expressionFor")
          .has(SampleType, "tumor")
          .out("sampleOf").as(individualStep)
          .select((signatureStep, expressionStep, individualStep)).toSet

        val query = highestQuery ++ lowestQuery

        val signatureData = query.map(_._1)
        val geneNames = expressionNames ++ signatureData.flatMap(takeHighest(5))
        val individualData = query.map(_._3)

        val expressionData = query.map { q =>
          val (sig, expression, individual) = q
          val coefficients = SignatureWorker.dehydrateCoefficients(expression.property("expressions").orElse(""))
          (individual.property("name").orElse(""), expression, coefficients)
        }

        val individualNames = individualData.map(_.property("name").orElse(""))
        val levelQuery = graph.V.hasLabel("individual")
          .has(Name, within(individualNames.toSeq:_*)).as(individualStep)
          .in("sampleOf").has(SampleType, "tumor")
          .in("expressionFor")
          .inE("appliesTo").as(levelStep)
          .outV.has(Name, within(signatureNames:_*)).as(signatureStep)
          .select((signatureStep, individualStep, levelStep)).toList

        val levelData = levelQuery.map { q =>
          val (signature, individual, level) = q
          (signature.property("name").orElse(""),
            individual.property("name").orElse(""),
            level.property("level").orElse(0.0))
        }.groupBy(_._1)

        // val levelData = expressionData.flatMap { expression =>
        //   val (individual, vertex, coefficients) = expression
        //   val levelQuery = vertex.inE("appliesTo").as(levelStep)
        //     .outV.filter((vertex) => signatureNames.contains(vertex.property("name").orElse(""))).as(signatureStep)
        //     .select((signatureStep, levelStep)).toSet

        //   levelQuery.map { q =>
        //     val (signature, edge) = q
        //     (signature.property("name").orElse(""), individual, edge.property("level").orElse(0.0))
        //   }
        // }.groupBy(_._1)

        val individualJson = clinicalNames.foldLeft(jEmptyArray) { (json, clinical) =>
          clinicalEvent(individualData.toList) (clinical) -->>: json
        }

        val expressionJson = geneNames.toSet.foldLeft(individualJson) { (json, gene) =>
          expressionEvent(expressionData.toList) (gene) -->>: json
        }

        val levelJson = levelData.foldLeft(expressionJson) { (json, score) =>
          val (signature, levelTuples) = score
          val levels = levelTuples.map(level => (level._2, level._3)).toMap
          levelEvent(levels) (signature) -->>: json
        }

        Ok(levelJson)
      }

    case request @ POST -> Root / "gaea" / "message" / messageType =>
      logger.info("importing " + messageType)
      val messages = request.bodyAsText.pipe(text.lines(1024 * 1024 * 64)).flatMap { line =>
        Process eval Ingest.ingestMessage(messageType) (graph) (line)
      } 
      messages.runLog.run
      Ingest.retryCommit(graph) (5)

      Ok(jString("done!"))

    case request @ POST -> Root / "yellow" =>
      val y = request.bodyAsText.pipe(text.lines()).flatMap { line =>
        Process eval puts(line)
      }
      y.runLog.run
      Ok(jNumber(1))
  }
}
