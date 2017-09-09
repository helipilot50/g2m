package net.helipilot50.graphql.export;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
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

import edu.emory.mathcs.backport.java.util.Collections;
import net.helipilot50.graphql.export.grammar.GraphQLBaseListener;
import net.helipilot50.graphql.export.grammar.GraphQLLexer;
import net.helipilot50.graphql.export.grammar.GraphQLParser;
import net.helipilot50.graphql.export.grammar.GraphQLParser.ArgumentsDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.DefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.DocumentContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.EnumTypeDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.EnumValueContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.EnumValueDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.FieldDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.ImplementsInterfacesContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.InputObjectTypeDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.InputValueDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.InterfaceTypeDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.ListTypeContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.NameContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.NonNullTypeContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.ObjectTypeDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.ScalarTypeDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.TypeContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.TypeDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.TypeNameContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.TypeSystemDefinitionContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.UnionTypeDefinitionContext;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

enum Language {
	PLANTUML,
	TEXTUML
}

public class IDLExport extends GraphQLBaseListener{
	private STGroup templates;
	private ParseTreeProperty<ST> code = new ParseTreeProperty<ST>();
	private GraphQLParser parser;
	private ParseTreeWalker walker = new ParseTreeWalker();
	private Set<String> reservedWords = new HashSet<String>();

	private List<FieldDefinitionContext> linkFields = new ArrayList<FieldDefinitionContext>();
	private Set<String> systemTypes = new HashSet<String>();
	
	Language language = null;
	
	boolean hideQuery = true;
	boolean hideMutation = true;
	boolean hideSubscription = true;

	private static Logger log = Logger.getLogger(IDLExport.class);

	public IDLExport() {
		super();
		systemTypes.add("int");
		systemTypes.add("float");
		systemTypes.add("string");
		systemTypes.add("boolean");

	}

	public void generate(String inputFileName, String outputFilePrefix, Language language) throws IOException{
		this.language = language;
		log.debug("Exporting file: " + inputFileName);
		org.antlr.v4.runtime.CharStream stream = CharStreams.fromFileName(inputFileName);
		GraphQLLexer lexer = new GraphQLLexer(stream);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		String umlCode = generate(tokens);

		switch (language){
		case PLANTUML: {
			/*
			 * PlantUML
			 */
			SourceStringReader reader = new SourceStringReader(umlCode);
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			// Write the first image to "os"
			String desc = reader.generateImage(os, new FileFormatOption(FileFormat.SVG));
			os.close();
			// The XML is stored into svg
			final String svg = new String(os.toByteArray(), Charset.forName("UTF-8"));
			File outputFile = new File(outputFilePrefix + ".svg");
			FileWriter fw = new FileWriter(outputFile);
			fw.write(svg);
			fw.close();
			outputFile = new File(outputFilePrefix + ".plantuml");
			fw = new FileWriter(outputFile);
			fw.write(umlCode);
			fw.close();
			break;
		}
		case TEXTUML: {
			/*
			 * TextUML
			 */
			File outputFile = new File(outputFilePrefix + ".tuml");
			FileWriter fw = new FileWriter(outputFile);
			fw.write(umlCode);
			fw.close();
			break;
		}
		default:
			// do nothing
			break;
		}
	}
	
	private void loadReservedWords() {
		reservedWords.clear();
		ST st = getTemplateFor("reservedWordList");
		String reserverWordsString = st.render();
		String[] wordArray = reserverWordsString.split("\n");
		Collections.addAll(reservedWords, wordArray);
	}
	private String generate(CommonTokenStream tokens){
		switch (language){
		case PLANTUML: {
			templates = new STGroupFile(getClass().getResource("plantuml.stg"), null, '$', '$');
			break;
		}
		case TEXTUML: {
			templates = new STGroupFile(getClass().getResource("textuml.stg"), null, '$', '$');
			break;
		}
		default:
			templates = new STGroupFile(getClass().getResource("textuml.stg"), null, '$', '$');
			break;
		}
		loadReservedWords();
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
//		if (hideQuery){
//			st.add("hide", "hide Query\n");
//		}
//		if (hideMutation){
//			st.add("hide", "hide Mutation\n");
//		}
//		if (hideSubscription){
//			st.add("hide", "hide Subscription\n");
//		}
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
		st.add("name", code.get(ctx.name()));
		//TODO members
		putCode(ctx, st);
	}
	@Override
	public void exitInterfaceTypeDefinition(InterfaceTypeDefinitionContext ctx) {
		ST st = getTemplateFor("interfaceTypeDefinition");
		st.add("name", code.get(ctx.name()));
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
		ST typeNameST = code.get(ctx.name());
		st.add("name", typeNameST);
		if (ctx.implementsInterfaces()!= null){
		for (TypeNameContext type : ctx.implementsInterfaces().typeName())
			st.add("interfaces", type.getText());
		}
		if (ctx.fieldDefinition()!=null){
			for (ParseTree field: ctx.fieldDefinition()){
				st.add("fields", code.get(field));
			}
		}
		for (FieldDefinitionContext fieldDefinition : linkFields){
			ST associationST = getTemplateFor("association");
			associationST.add("typeA", typeNameST);

			if (fieldDefinition.type().listType() !=null){
				String typeName = fieldDefinition.type().listType().type().getText();
				
				associationST.add("typeB", templateForReservedWord(typeName));

			} else if (fieldDefinition.type().nonNullType() !=null) {
				associationST.add("typeB", code.get(fieldDefinition.type().nonNullType()));
			} else
				//associationST.add("typeB", fieldDefinition.type().getText());
				associationST.add("typeB", code.get(fieldDefinition.type()));
			String backwardName = typeNameST.render().toLowerCase();
			associationST.add("nameB", templateForReservedWord(backwardName));
			associationST.add("nameA", code.get(fieldDefinition.name()));
			st.add("linkFields", associationST);

			ST methodST = getTemplateFor("operation");
			methodST.add("name", code.get(fieldDefinition.name()));
			if (fieldDefinition.argumentsDefinition()!=null){
				for (InputValueDefinitionContext argDef: fieldDefinition.argumentsDefinition().inputValueDefinition()){
					methodST.add("arguments", code.get(argDef));
				}
			}
			if (fieldDefinition.type().listType() !=null){
				methodST.add("type", code.get(fieldDefinition.type().listType()));
			} else
				methodST.add("type", fieldDefinition.type().getText());
			st.add("methods", methodST);

		}
		putCode(ctx, st);
	}
	
//	@Override
//	public void exitImplementsInterfaces(ImplementsInterfacesContext ctx) {
//		ST st = getTemplateFor("implementsInterface");
//	}

	@Override
	public void exitFieldDefinition(FieldDefinitionContext ctx) {
		TypeContext typeCtx = ctx.type();
		boolean systemType = false;
		if (typeCtx.listType()!=null)
			systemType = isSystemType(typeCtx.listType().type().typeName().getText());
		else if (typeCtx.nonNullType()!=null)
			systemType = isSystemType(typeCtx.nonNullType().typeName().getText());
		else 
			systemType = isSystemType(typeCtx.typeName().getText());

		/*
		 * if the field type is not a scalar, create a link to the type
		 */
		if (!systemType)
			linkFields.add(ctx);
		else {
			ST st = getTemplateFor("fieldDefinition");
			st.add("name", code.get(ctx.name()));
			st.add("type", code.get(typeCtx));
			putCode(ctx, st);
		}

	}

	@Override
	public void exitArgumentsDefinition(ArgumentsDefinitionContext ctx) {
		ST st = getTemplateFor("argumentsDefinition");
		if (ctx.inputValueDefinition()!=null){
			for (ParseTree inputValueDefinition: ctx.inputValueDefinition()){
				st.add("arguments", code.get(inputValueDefinition));
			}
		}
		putCode(ctx, st);
	}

	@Override
	public void exitInputValueDefinition(InputValueDefinitionContext ctx) {
		ST st = getTemplateFor("inputValueDefinition");
		st.add("name", code.get(ctx.name()));
		st.add("type", code.get(ctx.type()));
		if (ctx.defaultValue()!=null){
			st.add("defaultValue", ctx.defaultValue().value().getText());
		}
		putCode(ctx, st);
	}

	@Override
	public void exitType(TypeContext ctx) {
		ST st = getTemplateFor("type");
		if (ctx.listType()!=null)
			st.add("type", code.get(ctx.listType()));
		else if (ctx.nonNullType()!=null)
			st.add("type", templateForSystemType(ctx.nonNullType().typeName().getText()));
		else 
			st.add("type", templateForSystemType(ctx.typeName().getText()));
		putCode(ctx, st);
	}

	@Override
	public void exitListType(ListTypeContext ctx) {
		ST st = getTemplateFor("listType");
		st.add("typeName", templateForSystemType(ctx.type().getText()));
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
		st.add("name", code.get(ctx.name()));
		if (ctx.inputValueDefinition()!=null){
			for (ParseTree inputValue: ctx.inputValueDefinition()){
				st.add("inputValues", code.get(inputValue));
			}
		}
		putCode(ctx, st);
	}

	@Override
	public void exitEnumTypeDefinition(EnumTypeDefinitionContext ctx) {
		ST st = getTemplateFor("enumTypeDefinition");
		st.add("name", code.get(ctx.name()));
		if (ctx.enumValueDefinition()!=null){
			for (ParseTree enumValue: ctx.enumValueDefinition()){
				st.add("enumValues", code.get(enumValue));
			}
		}
		//String enumString = st.render();
		putCode(ctx, st);
	}
	
	@Override
	public void exitEnumValueDefinition(EnumValueDefinitionContext ctx) {
		putCode(ctx, code.get(ctx.enumValue()));
	}

	@Override
	public void exitEnumValue(EnumValueContext ctx) {
		ST st = getTemplateFor("enumValue");
		st.add("value", code.get(ctx.name()));
		putCode(ctx, st);
	}

	@Override
	public void exitScalarTypeDefinition(ScalarTypeDefinitionContext ctx) {
		ST st = getTemplateFor("scalarTypeDefinition");
		st.add("name", code.get(ctx.name()));
		putCode(ctx, st);
	}

	@Override
	public void exitName(NameContext ctx) {
		String name = ctx.getText();
		putCode(ctx, templateForReservedWord(name));
	}
	
	private ST templateForSystemType(String typeName){
		ST st = null;
		if (isSystemType(typeName)){
			switch (typeName.toLowerCase()) {
			case "int":
				st = getTemplateFor("int");
				break;
			case "string":
				st = getTemplateFor("string");
				break;
			case "boolean":
				st = getTemplateFor("boolean");
				break;
			case "float":
				st = getTemplateFor("float");
				break;
			}
			
		} else {
			st = getTemplateFor("customType");
			st.add("typeName", typeName);
		}
		return st;
	}
	private ST templateForReservedWord(String word){
		ST st = null;
		if (reservedWords.contains(word) ){
			st = getTemplateFor("reservedWord");
		} else {
			st = getTemplateFor("normalWord");
		}
		st.add("word", word);
		return st;
	}

}
