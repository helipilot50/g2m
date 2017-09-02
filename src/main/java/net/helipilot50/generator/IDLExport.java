package net.helipilot50.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.log4j.Logger;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import graphql.parser.antlr.GraphQLBaseListener;
import graphql.parser.antlr.GraphQLLexer;
import graphql.parser.antlr.GraphQLParser;
import graphql.parser.antlr.GraphQLParser.FieldDefinitionContext;
import graphql.parser.antlr.GraphQLParser.ObjectTypeDefinitionContext;



public class IDLExport extends GraphQLBaseListener{
	private STGroup templates = new STGroupFile(getClass().getResource("plantuml.stg"), null, '<', '>');
	private ParseTreeProperty<ST> code = new ParseTreeProperty<ST>();
	private GraphQLParser parser;
	private ParseTreeWalker walker = new ParseTreeWalker();
		
	private static Logger log = Logger.getLogger(IDLExport.class);
	
	public IDLExport() {
		super();
	}
	public void generate(File inputFile, File outputFile) throws IOException{
		log.debug("Generating file: " + inputFile.toString());
		String name = outputFile.getName();
		name = name.substring(0, name.lastIndexOf('.'));
		CommonTokenStream tokens = getTokenStream(new NoCaseFileStream(inputFile));
		String code = generate(tokens, name);
		FileWriter fw = new FileWriter(outputFile);
		fw.write(code);
		fw.close();
	}
	private String generate(CommonTokenStream tokens, String name){
		parser = new GraphQLParser(tokens);
		ParseTree tree = parser.document();
		walker.walk(this, tree);
		String exportText = code.get(tree).render();
		log.debug("Exported:\n" + exportText);
		return exportText;
	}
	private CommonTokenStream getTokenStream(ANTLRInputStream is){
		GraphQLLexer lexer = new GraphQLLexer(is);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		return tokens;
	}

	private ST getTemplateFor(String name){
		ST st = templates.getInstanceOf(name);
		return st;
	}
	private void putCode(ParseTree ctx, ST st){
		log.debug("Rendered: " + st.render());
		code.put(ctx, st);
	}

	@Override
	public void enterObjectTypeDefinition(ObjectTypeDefinitionContext ctx) {
		ST st = getTemplateFor("enterObjectTypeDefinition");
		st.add("name", ctx.name().getText());
		putCode(ctx, st);
		
	}
	@Override
	public void exitObjectTypeDefinition(ObjectTypeDefinitionContext ctx) {
		ST st = getTemplateFor("exitObjectTypeDefinition");
		putCode(ctx, st);
	}
	
	@Override
	public void enterFieldDefinition(FieldDefinitionContext ctx) {
		ST st = getTemplateFor("enterFieldDefinition");
		st.add("name", ctx.name().getText());
		putCode(ctx, st);
		
	}
	

}
