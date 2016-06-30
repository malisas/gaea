import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import bmeg.gaea.schema.Sample.*;
// import com.google.protobuf.util.JsonFormat;

public class ParsedDescriptorTest {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage:  ParsedDescriptorTest FILEDESCRIPTORSET_FILE");
            System.exit(-1);
	}

        ParsedDescriptor parsedDescriptor = new ParsedDescriptor(args[0]);
    
	System.out.println("\n\n\n* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n\n\n");

        // Get the hashmap of messages from the parsed descriptor and then print/hook up to reflection API
	HashMap<String, MessageAsNode> messages = parsedDescriptor.getMessages();
	/*for (MessageAsNode message: messages.values()) {
	    System.out.println(message.toString());
	}*/

	// Let's suppose you read in a json-serialized protobuf message, and you know that it is of type VariantCallEffect.
	String vce_json ="{ \"info\": { \"annotation_transcript\": \"uc010snu.1\", \"other_transcripts\": \"EIF4B_uc001sbh.3_Missense_Mutation_p.D563E|EIF4B_uc010snv.1_Missense_Mutation_p.D524E|EIF4B_uc001sbi.2_Missense_Mutation_p.D316E\", \"transcript_strand\": \"+\", \"swissprot_acc_id\": \"P23588\", \"refseq_mrna_id\": \"NM_001417\", \"uniprot_region\": \"\", \"protein_change\": \"p.D568E\", \"cdna_change\": \"c.1704T>A\", \"dbsnp_rs\": \"\", \"method\": \"Hybrid_Capture\", \"x46vertebrates_aa_alignment_column\": \"DD-DDDD--DDDDDD-DDDDDDDDDDDDDDD-D-------D--DE-\", \"uniprot_site\": \"\", \"refseq_prot_id\": \"NP_001408\", \"description\": \"eukaryotic translation initiation factor 4B\", \"alternative_allele_reads_count\": \"62\", \"dbsnp_val_status\": \"\", \"reference_allele_reads_count\": \"60\", \"swissprot_entry_id\": \"IF4B_HUMAN\", \"uniprot_aapos\": \"563\", \"codon_change\": \"c.(1702-1704)GAT>GAA\" }, \"source\": \"CCLE\", \"inFeatureEdges\": [ \"feature:EIF4B\" ], \"variantClassification\": \"Missense_Mutation\", \"effectOfEdges\": [ \"variantCall:CCLEbiosample:CCLE-PCM6_HAEMATOPOIETIC_AND_LYMPHOID_TISSUEposition:125343342053433420+SNPNA\" ], \"name\": \"variantCallEffect:variantCall:CCLEbiosample:CCLE-PCM6_HAEMATOPOIETIC_AND_LYMPHOID_TISSUEposition:125343342053433420+SNPNA\"}";

	/*
	Sample.VariantCallEffect.Builder vce = Sample.VariantCallEffect.newBuilder();
	JsonFormat.parser().merge(vce_json, vce);
	vce.build();

	// Now we have an instance of a VariantCallEffect object, and we want to extract information from it.

	// If you know what methods you can call, it is easy-peasy:
	System.out.println("Name: " + vce.getName());
	System.out.println("Source: " + vce.getSource());
	System.out.println("VariantClassification: " + vce.getVariantClassification());
	System.out.println("InFeatureEdges: " + vce.getInFeatureEdgesList()); // repeated field
	System.out.println("EffectOfEdges: " + vce.getEffectOfEdgesList()); // repeated field
	System.out.println("Info: " + vce.getInfo()); //a map
	*/

	// But if we don't/don't want to hard-code it manually, we should use the Reflection API.
	// Get the MessageAsNode information for VariantCallEffect
	MessageAsNode vceMessageAsNode = messages.get("VariantCallEffect");
	System.out.println(vceMessageAsNode);
	// .....unfinished...


    }
}
