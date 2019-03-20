package main.java.NLP;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;


public class NLP_Processor {
    private static StanfordCoreNLP pipeline;
    private static Boolean initWithOpenIE;
    private static NLP_Processor instance;

    public NLP_Processor() {
        initPipeline(true);
    }

    public static NLP_Processor getInstance() {
        if (instance == null) {
            instance = new NLP_Processor();
        }
        return instance;
    }


    // Won't retrieve relationships between entities in each sentence. Run
    // faster.
    public NLP_Document.Paragraph convertToNL_DocumentWithOutOpenIE(String para_text) {
        return convertToNL_Document(para_text, false);
    }

    // Retrieve relationships between entities in each sentence.
    public NLP_Document.Paragraph convertToNL_DocumentWithOpenIE(String para_text) {
        return convertToNL_Document(para_text, true);
    }

    public NLP_Document.Paragraph convertToNL_Document(String para_text, Boolean openie) {

        // logger.debug("Processing text with stanford NLP... ");

        NLP_Document.Paragraph para = new NLP_Document.Paragraph();
        CoreDocument document = new CoreDocument(para_text);

        pipeline.annotate(document);

        // Convert paragraph text into a list of sentences
        List<CoreSentence> coreSentences = document.sentences();
        ArrayList<NLP_Document.Sentence> sentences = new ArrayList<>();

        for (CoreSentence entryLine : coreSentences) {
            // Iterate through each sentence
            NLP_Document.Sentence sentence = new NLP_Document.Sentence();
            ArrayList<NLP_Document.Word> allNouns = new ArrayList<NLP_Document.Word>();
            ArrayList<NLP_Document.Word> allVerbs = new ArrayList<NLP_Document.Word>();

            String sentConent = entryLine.text();
            // logger.debug("Sentence: " + sentConent);
            // Iterate through each word in a sentence
            for (CoreLabel token : entryLine.tokens()) {
                NLP_Document.Word word = new NLP_Document.Word();
                word.setText(token.word());
                String pos = token.tag();
                word.setPosTag(pos);
                if (isNoun(pos)) {
                    allNouns.add(word);
                } else if (isVerb(pos)) {
                    allVerbs.add(word);
                } else {
                    // Ignore
                }
            }
            sentence.setSentContent(sentConent);
            sentence.setAllNouns(allNouns);
            sentence.setAllVerbs(allVerbs);

            if (openie) {
                // Retrieve dependency graph.
                SemanticGraph dependencies = entryLine.dependencyParse();
                sentence.setDependencyGraph(dependencies);

                // Retrieve all relations
                Collection<RelationTriple> triples = entryLine.coreMap()
                        .get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
                sentence.setTriples(new ArrayList<RelationTriple>(triples));

            }

            sentences.add(sentence);
        }

        para.setParaContent(para_text);
        para.setSentences(sentences);

        return para;
    }

    // Check if the word is noun
    private Boolean isNoun(String pos) {
        // POS: NN, NNS, NNP, NNPS
        return pos.toUpperCase().contains("NN");
    }

    // Check if the word is verb
    private Boolean isVerb(String pos) {
        // POS: VB, VBD, VBG, VBN, VBP, VBZ
        return pos.toUpperCase().contains("VB");
    }

    /**
     * @param openie
     *            Please try to initialize the pipeline once. There will be a
     *            time cost & logs when re-init the pipeline. If the pipeline
     *            already exists and no change on openIE option, then skip the
     *            initialization.
     */
    private static void initPipeline(Boolean openie) {

        String annotators = "tokenize,ssplit,pos";
        if (pipeline == null) {
            if (openie) {
                annotators = "tokenize,ssplit,pos,lemma,depparse,natlog,openie";
                initWithOpenIE = openie;
                System.out.println("Initialize NLP pipeline with OpenIE.");
            } else {
                initWithOpenIE = openie;
                System.out.println("Initialize Standard NLP pipeline.");

            }

            Properties props = new Properties();
            props.put("annotators", annotators);
            pipeline = new StanfordCoreNLP(props);
            System.out.println("Pipeline initialized.");
        } else {
            if (openie != initWithOpenIE) {
                if (openie) {
                    annotators = "tokenize,ssplit,pos,lemma,depparse,natlog,openie";
                    initWithOpenIE = openie;
                    System.out.println("Re-Initialize NLP pipeline with OpenIE.");
                } else {
                    initWithOpenIE = openie;
                    System.out.println("Re-Initialize Standard NLP pipeline without OpenIE.");

                }
                Properties props = new Properties();
                props.put("annotators", annotators);
                pipeline = new StanfordCoreNLP(props);
                System.out.println("Pipeline initialized.");
            }
        }
    }
}
