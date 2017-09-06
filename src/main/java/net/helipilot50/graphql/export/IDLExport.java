package net.helipilot50.graphql.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.log4j.Logger;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import net.helipilot50.graphql.export.grammar.GraphQLBaseListener;
import net.helipilot50.graphql.export.grammar.GraphQLLexer;
import net.helipilot50.graphql.export.grammar.GraphQLParser;
import net.helipilot50.graphql.export.grammar.GraphQLParser.DefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.DocumentContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.EnumTypeDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.EnumValueContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.FieldDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.InputObjectTypeDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.InputValueDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.InterfaceTypeDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.ListTypeContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.NonNullTypeContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.ObjectTypeDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.ScalarTypeDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.TypeContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.TypeDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.TypeSystemDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.UnionTypeDefinitionContext;



public class IDLExport extends GraphQLBaseListener{
	private STGroup templates = new STGroupFile(getClass().getResource("plantuml.stg"), null, '$', '$');
	private ParseTreeProperty<ST> code = new ParseTreeProperty<ST>();
	private GraphQLParser parser;
	private ParseTreeWalker walker = new ParseTreeWalker();
	
	private List<FieldDefinitionContext> linkFields = new ArrayList<FieldDefinitionContext>();
	private Set<String> systemTypes = new HashSet<String>();

	private static Logger log = Logger.getLogger(IDLExport.class);

	public IDLExport() {
		super();
		systemTypes.add("int");
		systemTypes.add("float");
		systemTypes.add("string");
		systemTypes.add("boolean");
		
	}
	
	public void generate(String inputFile, File outputFile) throws IOException{
		log.debug("Generating file: " + inputFile.toString());
		String name = outputFile.getName();
		name = name.substring(0, name.lastIndexOf('.'));
		org.antlr.v4.runtime.CharStream stream = CharStreams.fromFileName(inputFile);
		GraphQLLexer lexer = new GraphQLLexer(stream);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
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

	private ST getTemplateFor(String name){
		ST st = templates.getInstanceOf(name);
		return st;
	}
	private void putCode(ParseTree ctx, ST st){
		if (st != null)
			log.debug("Rendered: " + st.render());
		code.put(ctx, st);
	}
	
	private boolean isSystemType(String typeName){
		return systemTypes.contains(typeName.toLowerCase());
	}

	@Override
	public void exitDocument(DocumentContext ctx) {
		ST st = getTemplateFor("exitDocument");
		if (ctx.definition()!=null){
			for (ParseTree def: ctx.definition()){
				ST st2 = code.get(def);
				if (st2 != null){
					st.add("definitions", st2);
				} 

			}
		}
		putCode(ctx, st);
	}

	@Override
	public void exitDefinition(DefinitionContext ctx) {
		putCode(ctx, code.get(ctx.getChild(0)));
	}
	@Override
	public void exitTypeSystemDefinition(TypeSystemDefinitionContext ctx) {
		putCode(ctx, code.get(ctx.getChild(0)));	
	}
	@Override
	public void exitTypeDefinition(TypeDefinitionContext ctx) {
		putCode(ctx, code.get(ctx.getChild(0)));
	}
	@Override
	public void exitUnionTypeDefinition(UnionTypeDefinitionContext ctx) {
		ST st = getTemplateFor("unionTypeDefinition");
		st.add("name", ctx.name().getText());
		//TODO members
		putCode(ctx, st);
	}
	@Override
	public void exitInterfaceTypeDefinition(InterfaceTypeDefinitionContext ctx) {
		ST st = getTemplateFor("interfaceTypeDefinition");
		st.add("name", ctx.name().getText());
		if (ctx.fieldDefinition()!=null){
			for (ParseTree field: ctx.fieldDefinition()){
				st.add("fields", code.get(field));
			}
		}
		putCode(ctx, st);
	}

	@Override
	public void enterObjectTypeDefinition(ObjectTypeDefinitionContext ctx) {
		linkFields.clear();
		super.enterObjectTypeDefinition(ctx);
	}
	@Override
	public void exitObjectTypeDefinition(ObjectTypeDefinitionContext ctx) {
		ST st = getTemplateFor("objectTypeDefinition");
		String typeName = ctx.name().getText();
		st.add("name", typeName);
		if (ctx.fieldDefinition()!=null){
			for (ParseTree field: ctx.fieldDefinition()){
				st.add("fields", code.get(field));
			}
		}
		for (FieldDefinitionContext fieldDefinition : linkFields){
			ST typeST = getTemplateFor("association");
			ST one2Many = getTemplateFor("oneToMany");
			ST zeroOrOne = getTemplateFor("zeroOrOne");
			typeST.add("typeA", typeName);
			//typeST.add("cardinalityA", zeroOrOne);
			
			if (fieldDefinition.type().listType() !=null){
				typeST.add("typeB", code.get(fieldDefinition.type().listType()));
				typeST.add("cardinalityB", one2Many);
			} else if (fieldDefinition.type().nonNullType() !=null) {
				typeST.add("typeB", code.get(fieldDefinition.type().nonNullType()));
				typeST.add("cardinalityB", zeroOrOne);
			} else
				typeST.add("typeB", fieldDefinition.type().getText());
			
			
			typeST.add("nameB", typeName.toLowerCase());
			typeST.add("nameA", fieldDefinition.name().getText());
			st.add("linkFields", typeST);
		}
		putCode(ctx, st);
	}
	
	@Override
	public void exitFieldDefinition(FieldDefinitionContext ctx) {
		TypeContext typeContext = ctx.type();
		/*
		 * if the field type is not a scalar, create a link to the type
		 */
		TypeContext typeCtx = ctx.type();
		boolean systemType = false;
		if (typeCtx.listType()!=null)
			systemType = isSystemType(typeCtx.listType().type().typeName().getText());
		else if (typeCtx.nonNullType()!=null)
			systemType = isSystemType(typeCtx.nonNullType().typeName().getText());
		else 
			systemType = isSystemType(typeCtx.typeName().getText());
		if (systemType){
			ST st = getTemplateFor("fieldDefinition");
			st.add("name", ctx.name().getText());
			st.add("type", code.get(typeCtx));
			putCode(ctx, st);
		} else {
			linkFields.add(ctx);
		} 

	}
	@Override
	public void exitType(TypeContext ctx) {
		ST st = getTemplateFor("exitTypeName");
		String typeName = "";
		if (ctx.listType()!=null)
			typeName = ctx.listType().type().typeName().getText();
		else if (ctx.nonNullType()!=null)
			typeName = ctx.nonNullType().typeName().getText();
		else 
			typeName = ctx.typeName().getText();
		st.add("name", typeName);
		putCode(ctx, st);
	}

	@Override
	public void exitListType(ListTypeContext ctx) {
		ST st = getTemplateFor("fieldListType");
		st.add("typeName", ctx.type().getText());
		putCode(ctx, st);
	}
	
	@Override
	public void exitNonNullType(NonNullTypeContext ctx) {
		ST st = getTemplateFor("nonNullType");
		st.add("name", ctx.typeName());
		putCode(ctx, st);
	}


	
	@Override
	public void exitInputObjectTypeDefinition(InputObjectTypeDefinitionContext ctx) {
		ST st = getTemplateFor("inputObjectTypeDefinition");
		st.add("name", ctx.name().getText());
		if (ctx.inputValueDefinition()!=null){
			for (ParseTree inputValue: ctx.inputValueDefinition()){
				st.add("inputValues", code.get(inputValue));
			}
		}
		putCode(ctx, st);
	}
	
	@Override
	public void exitInputValueDefinition(InputValueDefinitionContext ctx) {
		ST st = getTemplateFor("inputFieldDefinition");
		st.add("name", ctx.name().getText());
		st.add("type", code.get(ctx.type()));
		if (ctx.defaultValue()!=null)
			st.add("defaultValue", ctx.defaultValue().getText());
		putCode(ctx, st);
	}

	@Override
	public void exitEnumTypeDefinition(EnumTypeDefinitionContext ctx) {
		ST st = getTemplateFor("enumTypeDefinition");
		st.add("name", ctx.name().getText());
		if (ctx.enumValueDefinition()!=null){
			for (ParseTree enumValue: ctx.enumValueDefinition()){
				st.add("enumValues", code.get(enumValue));
			}
		}
		putCode(ctx, st);
	}
	
	@Override
	public void exitEnumValue(EnumValueContext ctx) {
		ST st = getTemplateFor("enumValue");
		st.add("value", ctx.name().getText());
		putCode(ctx, st);
	}

	@Override
	public void exitScalarTypeDefinition(ScalarTypeDefinitionContext ctx) {
		ST st = getTemplateFor("scalarTypeDefinition");
		st.add("name", ctx.name().getText());
		putCode(ctx, st);
	}


}
