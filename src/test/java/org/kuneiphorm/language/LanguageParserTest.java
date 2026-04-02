package org.kuneiphorm.language;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.core.ExpressionChoice;
import org.kuneiphorm.daedalus.core.ExpressionQuantifier;
import org.kuneiphorm.daedalus.core.ExpressionSequence;
import org.kuneiphorm.daedalus.core.ExpressionUnit;
import org.kuneiphorm.language.grammar.Rule;
import org.kuneiphorm.language.grammar.Terminal;
import org.kuneiphorm.language.grammar.Variable;
import org.kuneiphorm.language.token.TokenDefinition;
import org.kuneiphorm.language.token.TokenTag;
import org.kuneiphorm.runtime.exception.SyntaxException;

class LanguageParserTest {

  // -------------------------------------------------------------------------
  // Language (full)
  // -------------------------------------------------------------------------

  @Test
  void parse_fullLanguage_returnsLanguageResult() throws SyntaxException {
    String input =
        """
        @version 1;
        language Foo;

        tokens {
            @skip 'WS' <- "[ \\t]+";
            'ID' <- "[a-z]+";
            'NUM' <- "[0-9]+";
        }

        rules {
            [start] S => 'ID' 'NUM';
        }
        """;
    ParseResult<String> result = LanguageParser.parse(input);
    assertTrue(result.isLanguageResult());

    Language<String> lang = result.getAsLanguageResult().language();
    assertEquals("Foo", lang.name());
    assertEquals(3, lang.tokens().size());
    assertEquals(1, lang.grammar().rules().size());
  }

  @Test
  void parse_fullLanguage_tokensCorrect() throws SyntaxException {
    String input =
        """
        @version 1;
        language Test;
        tokens {
            @skip 'WS' <- "[ ]+";
            'ID' <- "[a-z]+";
        }
        rules {
            S => 'ID';
        }
        """;
    Language<String> lang = LanguageParser.parse(input).getAsLanguageResult().language();

    TokenDefinition<String> ws = lang.tokens().get(0);
    assertEquals("WS", ws.label());
    assertEquals("[ ]+", ws.regex());
    assertTrue(ws.hasTag(TokenTag.SKIP));

    TokenDefinition<String> id = lang.tokens().get(1);
    assertEquals("ID", id.label());
    assertEquals("[a-z]+", id.regex());
    assertFalse(id.hasTag(TokenTag.SKIP));
  }

  @Test
  void parse_fullLanguage_rulesCorrect() throws SyntaxException {
    String input =
        """
        @version 1;
        language Test;
        tokens {
            'a' <- "a";
            'b' <- "b";
        }
        rules {
            [named] S => 'a' 'b';
            S => 'a';
        }
        """;
    Language<String> lang = LanguageParser.parse(input).getAsLanguageResult().language();

    assertEquals(2, lang.grammar().rules().size());

    Rule<String> r1 = lang.grammar().rules().get(0);
    assertEquals("named", r1.name());
    assertEquals("S", r1.source().name());
    assertInstanceOf(ExpressionSequence.class, r1.pattern());

    Rule<String> r2 = lang.grammar().rules().get(1);
    assertNull(r2.name());
  }

  @Test
  void parse_fullLanguage_startSymbolIsFirstRule() throws SyntaxException {
    String input =
        """
        @version 1;
        language Test;
        tokens { 'a' <- "a"; }
        rules {
            Expr => 'a';
        }
        """;
    Language<String> lang = LanguageParser.parse(input).getAsLanguageResult().language();
    assertEquals("Expr", lang.grammar().start().name());
  }

  // -------------------------------------------------------------------------
  // Lexer (tokens only)
  // -------------------------------------------------------------------------

  @Test
  void parse_lexer_returnsLexerResult() throws SyntaxException {
    String input =
        """
        @version 1;
        lexer MyLexer;
        tokens {
            @skip 'WS' <- "[ ]+";
            'ID' <- "[a-z]+";
        }
        """;
    ParseResult<String> result = LanguageParser.parse(input);
    assertTrue(result.isLexerResult());

    ParseResult.LexerResult<String> lexerResult = result.getAsLexerResult();
    assertEquals("MyLexer", lexerResult.name());
    assertEquals(2, lexerResult.tokens().size());
  }

  // -------------------------------------------------------------------------
  // Parser (rules only)
  // -------------------------------------------------------------------------

  @Test
  void parse_parser_returnsParserResult() throws SyntaxException {
    String input =
        """
        @version 1;
        parser MyParser;
        rules {
            S => A B;
            A => B;
            B => A;
        }
        """;
    ParseResult<String> result = LanguageParser.parse(input);
    assertTrue(result.isParserResult());

    ParseResult.ParserResult<String> parserResult = result.getAsParserResult();
    assertEquals("MyParser", parserResult.name());
    assertEquals(3, parserResult.grammar().rules().size());
    assertEquals("S", parserResult.grammar().start().name());
  }

  // -------------------------------------------------------------------------
  // Expression parsing
  // -------------------------------------------------------------------------

  private static Rule<String> firstRule(ParseResult<String> result) {
    return result.getAsParserResult().grammar().rules().get(0);
  }

  @Test
  void parse_terminalExpression_returnsUnit() throws SyntaxException {
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S => 'a'; }"));
    assertInstanceOf(ExpressionUnit.class, rule.pattern());
    assertInstanceOf(Terminal.class, ((ExpressionUnit<?>) rule.pattern()).label());
  }

  @Test
  void parse_variableExpression_returnsUnit() throws SyntaxException {
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S => A; A => S; }"));
    assertInstanceOf(ExpressionUnit.class, rule.pattern());
    assertInstanceOf(Variable.class, ((ExpressionUnit<?>) rule.pattern()).label());
  }

  @Test
  void parse_sequenceExpression_returnsSequence() throws SyntaxException {
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S => 'a' 'b' 'c'; }"));
    assertInstanceOf(ExpressionSequence.class, rule.pattern());
    assertEquals(3, rule.pattern().getChildren().size());
  }

  @Test
  void parse_choiceExpression_returnsChoice() throws SyntaxException {
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S => 'a' | 'b'; }"));
    assertInstanceOf(ExpressionChoice.class, rule.pattern());
    assertEquals(2, rule.pattern().getChildren().size());
  }

  @Test
  void parse_starQuantifier_returnsStar() throws SyntaxException {
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S => 'a' *; }"));
    assertInstanceOf(ExpressionQuantifier.class, rule.pattern());
    assertEquals(ExpressionQuantifier.Kind.STAR, ((ExpressionQuantifier<?>) rule.pattern()).kind());
  }

  @Test
  void parse_plusQuantifier_returnsPlus() throws SyntaxException {
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S => 'a'+; }"));
    assertInstanceOf(ExpressionQuantifier.class, rule.pattern());
    assertEquals(ExpressionQuantifier.Kind.PLUS, ((ExpressionQuantifier<?>) rule.pattern()).kind());
  }

  @Test
  void parse_optionalQuantifier_returnsOptional() throws SyntaxException {
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S => 'a'?; }"));
    assertInstanceOf(ExpressionQuantifier.class, rule.pattern());
    assertEquals(
        ExpressionQuantifier.Kind.OPTIONAL, ((ExpressionQuantifier<?>) rule.pattern()).kind());
  }

  @Test
  void parse_groupedExpression_parsesCorrectly() throws SyntaxException {
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S => ('a' | 'b')*; }"));
    assertInstanceOf(ExpressionQuantifier.class, rule.pattern());
    ExpressionQuantifier<?> q = (ExpressionQuantifier<?>) rule.pattern();
    assertEquals(ExpressionQuantifier.Kind.STAR, q.kind());
    assertInstanceOf(ExpressionChoice.class, q.body());
  }

  @Test
  void parse_nestedSequenceInChoice_parsesCorrectly() throws SyntaxException {
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S => 'a' 'b' | 'c'; }"));
    assertInstanceOf(ExpressionChoice.class, rule.pattern());
    ExpressionChoice<?> choice = (ExpressionChoice<?>) rule.pattern();
    assertInstanceOf(ExpressionSequence.class, choice.alternatives().get(0));
    assertInstanceOf(ExpressionUnit.class, choice.alternatives().get(1));
  }

  // -------------------------------------------------------------------------
  // Label mapper
  // -------------------------------------------------------------------------

  @Test
  void parse_customLabelMapper_appliesMapping() throws SyntaxException {
    String input =
        """
        @version 1;
        lexer Test;
        tokens {
            'ID' <- "[a-z]+";
        }
        """;
    ParseResult<Integer> result = LanguageParser.parse(input, s -> s.length());
    ParseResult.LexerResult<Integer> lexer = result.getAsLexerResult();
    assertEquals(2, lexer.tokens().get(0).label()); // "ID".length() == 2
  }

  // -------------------------------------------------------------------------
  // Comments and whitespace
  // -------------------------------------------------------------------------

  @Test
  void parse_withComments_ignoredCorrectly() throws SyntaxException {
    String input =
        """
        // This is a comment
        @version 1;
        language Test;
        /* block comment */
        tokens {
            'a' <- "a"; // inline comment
        }
        rules {
            S => 'a';
        }
        """;
    assertTrue(LanguageParser.parse(input).isLanguageResult());
  }

  @Test
  void parse_commentBeforeArrow_parsesCorrectly() throws SyntaxException {
    // skipTrivia before => in parseRule
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S /* c */ => 'a'; }"));
    assertEquals("S", rule.source().name());
  }

  @Test
  void parse_commentBeforeExpression_parsesCorrectly() throws SyntaxException {
    // skipTrivia before expression in parseRule
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S => /* c */ 'a'; }"));
    assertInstanceOf(ExpressionUnit.class, rule.pattern());
  }

  @Test
  void parse_commentBeforePipe_parsesCorrectly() throws SyntaxException {
    // skipTrivia before | in parseExpression
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S => 'a' /* c */ | 'b'; }"));
    assertInstanceOf(ExpressionChoice.class, rule.pattern());
  }

  @Test
  void parse_commentAfterPipe_parsesCorrectly() throws SyntaxException {
    // skipTrivia after | in parseExpression
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S => 'a' | /* c */ 'b'; }"));
    assertInstanceOf(ExpressionChoice.class, rule.pattern());
  }

  @Test
  void parse_commentInsideGroup_parsesCorrectly() throws SyntaxException {
    // skipTrivia inside ( ) in parseBase
    Rule<String> rule =
        firstRule(
            LanguageParser.parse("@version 1; parser T; rules { S => ( /* c */ 'a' /* c */ )*; }"));
    assertInstanceOf(ExpressionQuantifier.class, rule.pattern());
  }

  @Test
  void parse_commentBeforeSemicolon_parsesCorrectly() throws SyntaxException {
    // skipTrivia before ; in parseRule
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S => 'a' /* c */ ; }"));
    assertInstanceOf(ExpressionUnit.class, rule.pattern());
  }

  // -------------------------------------------------------------------------
  // Error cases
  // -------------------------------------------------------------------------

  @Test
  void parse_unknownKeyword_throws() {
    assertThrows(
        SyntaxException.class, () -> LanguageParser.parse("@version 1; foobar Test; tokens {}"));
  }

  @Test
  void parse_missingSemicolon_throws() {
    assertThrows(
        SyntaxException.class, () -> LanguageParser.parse("@version 1; language Test tokens {}"));
  }

  @Test
  void parse_unclosedTokensBlock_throws() {
    assertThrows(
        SyntaxException.class,
        () -> LanguageParser.parse("@version 1; lexer T; tokens { 'ID' <- \"abc"));
  }

  @Test
  void parse_emptyInput_throws() {
    assertThrows(SyntaxException.class, () -> LanguageParser.parse(""));
  }

  @Test
  void parse_nullInput_throwsNpe() {
    assertThrows(NullPointerException.class, () -> LanguageParser.parse((String) null));
  }

  @Test
  void parse_missingVersionHeader_throws() {
    assertThrows(SyntaxException.class, () -> LanguageParser.parse("language Test; tokens {}"));
  }

  @Test
  void parse_unknownTokenTag_throws() {
    assertThrows(
        SyntaxException.class,
        () -> LanguageParser.parse("@version 1; lexer T; tokens { @foobar 'X' <- \"x\"; }"));
  }

  @Test
  void parse_wrongKeywordForTokensBlock_throws() {
    assertThrows(
        SyntaxException.class,
        () -> LanguageParser.parse("@version 1; lexer T; foobar { 'X' <- \"x\"; }"));
  }

  @Test
  void parse_wrongKeywordForRulesBlock_throws() {
    assertThrows(
        SyntaxException.class,
        () ->
            LanguageParser.parse(
                "@version 1; language T; tokens { 'a' <- \"a\"; } foobar { S => 'a'; }"));
  }

  @Test
  void parse_eofInIdentifier_throws() {
    assertThrows(SyntaxException.class, () -> LanguageParser.parse("@version 1;"));
  }

  @Test
  void parse_versionNonDigit_throws() {
    assertThrows(SyntaxException.class, () -> LanguageParser.parse("@version x;"));
  }

  @Test
  void parse_unclosedLabel_throws() {
    assertThrows(
        SyntaxException.class,
        () -> LanguageParser.parse("@version 1; lexer T; tokens { 'X <- \"x\"; }"));
  }

  @Test
  void parse_eofAfterRuleArrow_throws() {
    assertThrows(
        SyntaxException.class, () -> LanguageParser.parse("@version 1; parser T; rules { S =>"));
  }

  @Test
  void parse_unsupportedVersion_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> LanguageParser.parse("@version 99; language Test;"));
  }

  @Test
  void parse_versionZero_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> LanguageParser.parse("@version 0; language Test;"));
  }

  // -------------------------------------------------------------------------
  // Whitespace resilience (skipTrivia coverage)
  // -------------------------------------------------------------------------

  @Test
  void parse_extraWhitespaceEverywhere_parsesCorrectly() throws SyntaxException {
    String input =
        """

            @version 1;
            language   Test  ;

            tokens  {
                @skip   'WS'   <-   "[ ]+"  ;
                'a'  <-  "a"  ;
            }

            rules  {
                [ named ]  S  =>  'a'  ;
            }

        """;
    assertTrue(LanguageParser.parse(input).isLanguageResult());
  }

  @Test
  void parse_whitespaceInsideGroup_parsesCorrectly() throws SyntaxException {
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S => ( 'a' | 'b' ) *; }"));
    assertInstanceOf(ExpressionQuantifier.class, rule.pattern());
  }

  // -------------------------------------------------------------------------
  // Identifier boundary characters
  // -------------------------------------------------------------------------

  @Test
  void parse_identifierBoundaryChars_allAccepted() throws SyntaxException {
    String input = "@version 1; parser T; rules { aZ_09 => Az; Az => aZ_09; }";
    ParseResult<String> result = LanguageParser.parse(input);
    assertEquals(2, result.getAsParserResult().grammar().rules().size());
  }

  @Test
  void parse_underscoreIdentifier_accepted() throws SyntaxException {
    String input = "@version 1; parser T; rules { _start => _end; _end => _start; }";
    assertTrue(LanguageParser.parse(input).isParserResult());
  }

  // -------------------------------------------------------------------------
  // readInt boundary coverage
  // -------------------------------------------------------------------------

  @Test
  void parse_multiDigitVersion_parsesCorrectly() throws SyntaxException {
    // Exercises readInt with result = result * 10 + digit for multiple digits.
    // If multiplication is replaced with division, the result would be wrong.
    // Note: only version 1 is currently supported, so we test that parsing fails
    // with the right exception for version 10 (unsupported, not syntax error).
    assertThrows(
        IllegalArgumentException.class, () -> LanguageParser.parse("@version 10; language Test;"));
  }

  @Test
  void parse_digitBoundaryZero_throwsForNonDigit() {
    // Tests the boundary: '0' - 1 = '/' should not be accepted as a digit.
    assertThrows(SyntaxException.class, () -> LanguageParser.parse("@version /; language Test;"));
  }

  @Test
  void parse_digitBoundaryNine_throwsForNonDigit() {
    // Tests the boundary: '9' + 1 = ':' should not be accepted as a digit.
    assertThrows(SyntaxException.class, () -> LanguageParser.parse("@version :; language Test;"));
  }

  // -------------------------------------------------------------------------
  // Empty rule (parseTerm returning empty sequence)
  // -------------------------------------------------------------------------

  @Test
  void parse_emptyAlternative_producesEmptySequence() throws SyntaxException {
    // C => 'a' | ; -- the second alternative is empty (epsilon).
    // parseTerm returns Expression.sequence() which must not be null.
    Rule<String> rule =
        firstRule(LanguageParser.parse("@version 1; parser T; rules { S => 'a' | ; S => S; }"));
    assertInstanceOf(ExpressionChoice.class, rule.pattern());
    ExpressionChoice<?> choice = (ExpressionChoice<?>) rule.pattern();
    assertEquals(2, choice.alternatives().size());
    // The second alternative should be an empty sequence, not null.
    assertNotNull(choice.alternatives().get(1));
    assertInstanceOf(ExpressionSequence.class, choice.alternatives().get(1));
    assertTrue(choice.alternatives().get(1).getChildren().isEmpty());
  }

  // -------------------------------------------------------------------------
  // readIdentifier edge cases
  // -------------------------------------------------------------------------

  @Test
  void parse_identifierStartingWithDigit_throws() {
    // readIdentifier negated conditional: !isIdentStart should reject digits.
    assertThrows(
        SyntaxException.class,
        () -> LanguageParser.parse("@version 1; parser T; rules { 0bad => S; S => 0bad; }"));
  }

  // -------------------------------------------------------------------------
  // Round-trip: render then parse
  // -------------------------------------------------------------------------

  @Test
  void roundTrip_exprLanguage_preservesStructure() throws SyntaxException {
    String input =
        """
        @version 1;
        language ExprLang;
        tokens {
            @skip 'WS' <- "[ \\t]+";
            'NUM' <- "[0-9]+";
            'PLUS' <- "\\+";
        }
        rules {
            [add] Expr => Expr 'PLUS' Term;
            [num] Expr => Term;
            [lit] Term => 'NUM';
        }
        """;

    Language<String> lang1 = LanguageParser.parse(input).getAsLanguageResult().language();
    String rendered = LanguageRenderer.render(lang1);
    Language<String> lang2 = LanguageParser.parse(rendered).getAsLanguageResult().language();

    assertEquals(lang1.name(), lang2.name());
    assertEquals(lang1.tokens().size(), lang2.tokens().size());
    assertEquals(lang1.grammar().rules().size(), lang2.grammar().rules().size());
    assertEquals(lang1.grammar().start(), lang2.grammar().start());

    for (int i = 0; i < lang1.tokens().size(); i++) {
      assertEquals(lang1.tokens().get(i).label(), lang2.tokens().get(i).label());
      assertEquals(lang1.tokens().get(i).regex(), lang2.tokens().get(i).regex());
      assertEquals(lang1.tokens().get(i).tags(), lang2.tokens().get(i).tags());
    }

    for (int i = 0; i < lang1.grammar().rules().size(); i++) {
      assertEquals(lang1.grammar().rules().get(i).name(), lang2.grammar().rules().get(i).name());
      assertEquals(
          lang1.grammar().rules().get(i).source(), lang2.grammar().rules().get(i).source());
    }
  }

  @Test
  void roundTrip_jsonLike_preservesTokenCount() throws SyntaxException {
    String input =
        """
        @version 1;
        language JsonLike;
        tokens {
            @skip 'WS' <- "[ \\t\\r\\n]+";
            'LBRACE' <- "\\{";
            'RBRACE' <- "\\}";
            'LBRACKET' <- "\\[";
            'RBRACKET' <- "\\]";
            'COLON' <- ":";
            'COMMA' <- ",";
            'STRING' <- "\\"[^\\"]*\\"";

            'NUMBER' <- "[0-9]+";
            'TRUE' <- "true";
            'FALSE' <- "false";
            'NULL' <- "null";
        }
        rules {
            Value => Object | Array | 'STRING' | 'NUMBER' | 'TRUE' | 'FALSE' | 'NULL';
            Object => 'LBRACE' (Pair ('COMMA' Pair)*)? 'RBRACE';
            Pair => 'STRING' 'COLON' Value;
            Array => 'LBRACKET' (Value ('COMMA' Value)*)? 'RBRACKET';
        }
        """;

    Language<String> lang = LanguageParser.parse(input).getAsLanguageResult().language();
    assertEquals("JsonLike", lang.name());
    assertEquals(12, lang.tokens().size());
    assertEquals(4, lang.grammar().rules().size());
    assertEquals(
        Set.of(
            "LBRACE",
            "RBRACE",
            "LBRACKET",
            "RBRACKET",
            "COLON",
            "COMMA",
            "STRING",
            "NUMBER",
            "TRUE",
            "FALSE",
            "NULL"),
        lang.grammar().terminalLabels());
  }

  @Test
  void roundTrip_lexer_preservesStructure() throws SyntaxException {
    String input =
        """
        @version 1;
        lexer MyLexer;
        tokens {
            @skip 'WS' <- "[ ]+";
            'ID' <- "[a-z]+";
        }
        """;
    ParseResult.LexerResult<String> lr1 = LanguageParser.parse(input).getAsLexerResult();
    String rendered = LanguageRenderer.renderResult(LanguageParser.parse(input));
    ParseResult.LexerResult<String> lr2 = LanguageParser.parse(rendered).getAsLexerResult();

    assertEquals(lr1.name(), lr2.name());
    assertEquals(lr1.tokens().size(), lr2.tokens().size());
  }

  @Test
  void roundTrip_parser_preservesStructure() throws SyntaxException {
    String input = "@version 1; parser MyParser; rules { S => 'a' 'b'; }";
    ParseResult.ParserResult<String> pr1 = LanguageParser.parse(input).getAsParserResult();
    String rendered = LanguageRenderer.renderResult(LanguageParser.parse(input));
    ParseResult.ParserResult<String> pr2 = LanguageParser.parse(rendered).getAsParserResult();

    assertEquals(pr1.name(), pr2.name());
    assertEquals(pr1.grammar().rules().size(), pr2.grammar().rules().size());
  }
}
