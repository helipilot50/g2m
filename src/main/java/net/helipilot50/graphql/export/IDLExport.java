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
import net.helipilot50.graphql.export.grammar.GraphQLParser.UnionMembersContext;
import net.helipilot50.graphql.export.grammar.GraphQLParser.UnionTypeDefinitionContext;

enum Language {
	PLANTUML,
	TEXTUML,
	PROTO,
	XMI
}

public class IDLExport extends GraphQLBaseListener{
	private STGroup templates;
	private ParseTreeProperty<ST> code = new ParseTreeProperty<ST>();
	private GraphQLParser parser;
	private ParseTreeWalker walker = new ParseTreeWalker();
	private Set<String> reservedWords = new HashSet<String>();

	private List<ST> linkFields = new ArrayList<ST>();
	private ST currentType = null;
	private Set<String> systemTypes = new HashSet<String>();

	Language language = null;
	private String packageName;

	private static Logger log = Logger.getLogger(IDLExport.class);

	public IDLExport() {
		super();
		systemTypes.add("int");
		systemTypes.add("float");
		systemTypes.add("string");
		systemTypes.add("boolean");
		// custom scalars
		systemTypes.add("jsontype");
		systemTypes.add("date");

	}

	public void generate(String inputFileName, String outputFilePrefix, Language language, String packageName) throws IOException{
		this.language = language;
		this.packageName = packageName;
		log.debug("Exporting file: " + inputFileName);
		org.antlr.v4.runtime.CharStream stream = CharStreams.fromFileName(inputFileName);
		GraphQLLexer lexer = new GraphQLLexer(stream);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		String umlCode = generate(tokens);

		switch (language){
		/*
		 * PlantUML
		 */
		case PLANTUML: {
			// write as SVG
			//			SourceStringReader reader = new SourceStringReader(umlCode);
			//			ByteArrayOutputStream os = new ByteArrayOutputStream();
			//			String desc = reader.generateImage(os, new FileFormatOption(FileFormat.SVG));
			//			os.close();
			//			final String svg = new String(os.toByteArray(), Charset.forName("UTF-8"));
			//			File outputFile = new File(outputFilePrefix + ".svg");
			//			FileWriter fw = new FileWriter(outputFile);
			//			fw.write(svg);
			//			fw.close();
			// write as puml text
			File outputFile = new File(outputFilePrefix + ".puml");
			FileWriter fw = new FileWriter(outputFile);
			fw.write(umlCode);
			fw.close();
			break;
		}
		/*
		 * TextUML
		 */
		case TEXTUML: {
			File outputFile = new File(outputFilePrefix + ".tuml");
			FileWriter fw = new FileWriter(outputFile);
			fw.write(umlCode);
			fw.close();
			break;
		}
		/*
		 * Proto Buf
		 */
		case PROTO: {
			File outputFile = new File(outputFilePrefix + ".proto");
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
		case PROTO: {
			templates = new STGroupFile(getClass().getResource("protobuf.stg"), null, '$', '$');
			break;
		}
		default:
			templates = new STGroupFile(getClass().getResource("plantuml.stg"), null, '$', '$');
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
			log.trace("Rendered: " + st.render());
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
		st.add("package", this.packageName);
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
		UnionMembersContext members = ctx.unionMembers();
		traverseUnionMembers(st, members);
		putCode(ctx, st);
	}
	private void traverseUnionMembers(ST unionST, UnionMembersContext ctx){
		if (ctx.typeName()!=null)
			unionST.add("members", code.get(ctx.typeName().name()));

		if (ctx.unionMembers()!= null)
			traverseUnionMembers(unionST, ctx.unionMembers());
	}
	@Override
	public void exitUnionMembers(UnionMembersContext ctx) {
		putCode(ctx, code.get(ctx.typeName()));
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
		currentType = templateForSystemType(ctx.name().getText());
		super.enterObjectTypeDefinition(ctx);
	}

	@Override
	public void exitObjectTypeDefinition(ObjectTypeDefinitionContext ctx) {
		ST st = getTemplateFor("objectTypeDefinition");
		ST typeName = templateForSystemType(ctx.name().getText());
		st.add("name", typeName);
		if (ctx.implementsInterfaces()!= null){
			for (TypeNameContext type : ctx.implementsInterfaces().typeName())
				st.add("interfaces", type.getText());
		}
		if (ctx.fieldDefinition()!=null){
			for (ParseTree field: ctx.fieldDefinition()){
				st.add("fields", code.get(field));
			}
		}
		linkFields(st, linkFields);
		putCode(ctx, st);
	}

	private void linkFields(ST st, List<ST> linkFields){
		for (ST associationST : linkFields){
			st.add("linkFields", associationST);
		}
	}

	private ST linkFieldST(ST typeName, ST methodName, String methodType, ST sourceCard, ST destCard){
		ST associationST = getTemplateFor("association");
		associationST.add("typeA", typeName);
		associationST.add("nameA", methodName);
		associationST.add("cardA", sourceCard);

		String backwardName = typeName.render().toLowerCase();
		associationST.add("nameB", templateForReservedWord(backwardName));
		associationST.add("typeB", templateForReservedWord(methodType));
		associationST.add("cardB", destCard);

		return associationST;
	}

	@Override
	public void enterInputObjectTypeDefinition(InputObjectTypeDefinitionContext ctx) {
		linkFields.clear();
		currentType = templateForSystemType(ctx.name().getText());
		super.enterInputObjectTypeDefinition(ctx);
	}
	@Override
	public void exitInputObjectTypeDefinition(InputObjectTypeDefinitionContext ctx) {
		ST st = getTemplateFor("inputObjectTypeDefinition");
		ST typeName = templateForSystemType(ctx.name().getText());
		st.add("name", typeName);
		if (ctx.inputValueDefinition()!=null){
			for (ParseTree inputValue: ctx.inputValueDefinition()){
				st.add("inputValues", code.get(inputValue));
			}
		}
		linkFields(st, linkFields);
		putCode(ctx, st);
	}


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
		if (systemType) {
			ST st = getTemplateFor("fieldDefinition");
			st.add("name", code.get(ctx.name()));
			st.add("type", code.get(typeCtx));
			putCode(ctx, st);
		} else {
			//its a method
			ST methodST = getTemplateFor("operation");
			ST methodName = code.get(ctx.name());
			methodST.add("name", methodName);
			if (ctx.argumentsDefinition()!=null){ 
				for (InputValueDefinitionContext argDef: ctx.argumentsDefinition().inputValueDefinition()){
					methodST.add("arguments", code.get(argDef));
				}
			}
			ST methodType = code.get(ctx.type());
			methodST.add("type", methodType);
			//String mt = methodType.render();
			putCode(ctx, methodST);
			
			
			// association
			createAssociation(linkFields, methodName, typeCtx);
		}
	}

	@Override
	public void exitInputValueDefinition(InputValueDefinitionContext ctx) {
		TypeContext typeCtx = ctx.type();
		boolean systemType = false;
		String typeName = typeNameFromContext(typeCtx);
		systemType = isSystemType(typeName);
		ST nameST = code.get(ctx.name());
		ST st = getTemplateFor("inputValueDefinition");
		st.add("name", nameST);
		if (systemType && (typeCtx.nonNullType()!=null) && (typeCtx.nonNullType().listType()!=null)) 
			st.add("type", code.get(typeCtx.nonNullType().listType()));
		else
			st.add("type", code.get(typeCtx));
		putCode(ctx, st);

		if (!systemType) {
			// association
			createAssociation(linkFields, nameST, typeCtx);
		}
	}

	private void createAssociation(List<ST> linkFields, ST nameST, TypeContext typeCtx) {
		// association
		ST cardB = null;
		String typeString = null;
		if (typeCtx.nonNullType()!=null) {
			if(typeCtx.nonNullType().listType()!=null)
				cardB = getTemplateFor("oneToMany");
			else
				cardB = getTemplateFor("exactlyOne");
			typeString = code.get(typeCtx).render();
		} else if (typeCtx.listType()!=null){
			cardB = getTemplateFor("zeroToMany");
			typeString = typeCtx.listType().type().getText();
		}else {
			cardB = getTemplateFor("zeroOrOne");
			typeString = typeCtx.getText();
		}
		ST linkST = linkFieldST(currentType, 
				nameST, 
				typeString, 
				getTemplateFor("exactlyOne"),
				cardB);
		linkFields.add(linkST);
		
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


	private String typeNameFromContext(TypeContext typeCtx) {
		if (typeCtx.listType()!=null) 
			return typeCtx.listType().type().typeName().getText();
		else if (typeCtx.nonNullType()!=null)  {
			if (typeCtx.nonNullType().listType()!=null) 
				return typeCtx.nonNullType().listType().type().typeName().getText();
			else
				return typeCtx.nonNullType().typeName().getText();
		}
		else 
			return typeCtx.typeName().getText();
	}

	private TypeContext typeFromContext(TypeContext typeCtx) {
		if (typeCtx.listType()!=null) 
			return typeCtx.listType().type();
		else if (typeCtx.nonNullType()!=null)  {
			if (typeCtx.nonNullType().listType()!=null) 
				return typeCtx.nonNullType().listType().type();
			else
				return typeCtx;
		}
		else 
			return typeCtx;
	}

	@Override
	public void exitType(TypeContext ctx) {
		//System.out.println(ctx.getText());
		ST st = getTemplateFor("type");
		if (ctx.listType()!=null)
			st.add("type", code.get(ctx.listType()));
		else if (ctx.nonNullType()!=null) {
			NonNullTypeContext nonNullCtx  = ctx.nonNullType();
			if (nonNullCtx.listType()==null) {
				TypeNameContext tnc = nonNullCtx.typeName();
				String name = tnc.getText();
				st.add("type", templateForSystemType(name));
			}
		}
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
		ST stx = templateForReservedWord(name);
		stx.render();
		putCode(ctx, stx);
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
			case "jsontype":
				st = getTemplateFor("jsontype");
				break;
			case "date":
				st = getTemplateFor("date");
				break;
			}

		} else {
			st = getTemplateFor("customType");
			//			ST seperatorST = getTemplateFor("packageSeperator");
			//			String seperator = seperatorST.render();
			//			String[] nameParts = typeName.split(seperator + "+");
			//			String name = nameParts[nameParts.length-1]; //last element is the name
			st.add("typeName", typeName);
			//			if (nameParts.length >1){ //if there is a package
			//				String[] packages = Arrays.copyOfRange(nameParts, 0, nameParts.length-2);
			//				st.add("package", packages);
			//			}
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
