package org.key_project.sed.key.evaluation.model.definition;

import java.net.URL;
import java.util.List;

import org.key_project.sed.key.evaluation.model.util.EvaluationModelImages;
import org.key_project.sed.key.evaluation.model.validation.FixedValueValidator;
import org.key_project.sed.key.evaluation.model.validation.IValueValidator;
import org.key_project.sed.key.evaluation.model.validation.NotUndefinedValueValidator;
import org.key_project.util.java.CollectionUtil;

public class ReviewingCodeEvaluation extends AbstractEvaluation {
   /**
    * The only instance of this class.
    */
   public static final ReviewingCodeEvaluation INSTANCE = new ReviewingCodeEvaluation();

   /**
    * The name of the {@link Tool} representing no tools.
    */
   public static final String NO_TOOL_NAME = "JDT Debugger";

   /**
    * The name of the {@link Tool} representing 'SED'.
    */
   public static final String SED_TOOL_NAME = "SED";

   /**
    * The name of the introduction form.
    */
   public static final String INTRODUCTION_FORM_NAME = "introductionForm";

   /**
    * The name of the used random order computer.
    */
   public static final String RANDOM_COMPUTER_NAME = "ReviewingCodeRandomFormOrderComputer";

   /**
    * Page name of the evaluation instruction page.
    */
   public static final String EVALUATION_PAGE_NAME = "evaluationInstructions";

   /**
    * Page name of the JML introduction page.
    */
   public static final String JML_PAGE_NAME = "JML";
   
   /**
    * Page name of example 1.
    */
   public static final String EXAMPLE_1_PAGE_NAME = "ObservableArray";

   /**
    * Page name of example 2.
    */
   public static final String EXAMPLE_2_PAGE_NAME = "BankUtil";

   /**
    * Page name of example 3.
    */
   public static final String EXAMPLE_3_PAGE_NAME = "IntegerUtil";

   /**
    * Page name of example 4.
    */
   public static final String EXAMPLE_4_PAGE_NAME = "MathUtil";

   /**
    * Page name of example 5.
    */
   public static final String EXAMPLE_5_PAGE_NAME = "ValueSearch";

   /**
    * Page name of example 6.
    */
   public static final String EXAMPLE_6_PAGE_NAME = "Stack";

   /**
    * Page name of the send evaluation page.
    */
   public static final String SEND_EVALUATION_PAGE_NAME = "sendEvaluation";

   /**
    * The name of the evaluation form.
    */
   public static final String EVALUATION_FORM_NAME = "evaluationForm";

   /**
    * Page name of the summary page.
    */
   public static final String FEEDBACK_PAGE = "feedback";
   
   /**
    * Forbid additional instances.
    */
   private ReviewingCodeEvaluation() {
      super("Reviewing Code", isUIAvailable() ? "data/reviewingCode/instructions/" : null);
   }
   
   @Override
   protected List<Tool> computeTools() {
      URL noToolURL = isUIAvailable() ? toLocalURL("data/reviewingCode/instructions/NoTool.html") : null;
      URL sedURL = isUIAvailable() ? toLocalURL("data/reviewingCode/instructions/SED.html") : null;
      Tool noTool = new Tool(NO_TOOL_NAME, 
                             noToolURL, 
                             noToolURL, 
                             isUIAvailable() ? EvaluationModelImages.getImage(EvaluationModelImages.JAVA_APPLICATION_LOGO) : null);
      Tool sed = new Tool(SED_TOOL_NAME, 
                          sedURL, 
                          sedURL, 
                          isUIAvailable() ? EvaluationModelImages.getImage(EvaluationModelImages.SED_LOGO) : null);
      return CollectionUtil.toList(noTool, sed);
   }

   @Override
   protected List<AbstractForm> computeForms() {
      // Create introduction form
      URL conditionsURL = isUIAvailable() ? toLocalURL("data/reviewingCode/instructions/conditions.html") : null;
      QuestionPage conditionsPage = new QuestionPage("conditionsPage", 
                                                     "Introduction", 
                                                     "Please read the information and conditions of the evaluation carefully.",
                                                     false,
                                                     false,
                                                     null,
                                                     new BrowserQuestion("conditions", conditionsURL),
                                                     new RadioButtonsQuestion("acceptConditions",
                                                                              null, 
                                                                              true,
                                                                              "no", 
                                                                              new FixedValueValidator("yes", "Please read and accept the information and conditions of the evaluation."), 
                                                                              false,
                                                                              new Choice("I &accept the conditions", "yes"), 
                                                                              new Choice("I do &not accept the conditions", "no")));
      QuestionPage backgroundPage = new QuestionPage("backgroundPage", 
                                                     "Background Knowledge", 
                                                     "Please fill out the form with your background knowledge.",
                                                     true,
                                                     false,
                                                     null,
                                                     new RadioButtonsQuestion("experienceWithJava",
                                                                              "Experience with Java", 
                                                                              true,
                                                                              null, 
                                                                              new NotUndefinedValueValidator("Experience with Java not defined."), 
                                                                              false,
                                                                              new Choice("None", "None"), 
                                                                              new Choice("< 2 years", "Less than 2 years"), 
                                                                              new Choice(">= 2 years", "More than 2 years")),
                                                     new RadioButtonsQuestion("experienceWithJML",
                                                                              "Experience with JML", 
                                                                              true,
                                                                              null, 
                                                                              new NotUndefinedValueValidator("Experience with JML not defined."), 
                                                                              false,
                                                                              new Choice("None", "None"), 
                                                                              new Choice("< 2 years", "Less than 2 years"), 
                                                                              new Choice(">= 2 years", "More than 2 years")),
                                                     new RadioButtonsQuestion("experienceWithSymbolicExecution",
                                                                              "Experience with symbolic execution (e.g. verification or test case generation)", 
                                                                              true,
                                                                              null, 
                                                                              new NotUndefinedValueValidator("Experience with symbolic execution not defined."), 
                                                                              false,
                                                                              new Choice("None", "None"), 
                                                                              new Choice("< 2 years", "Less than 2 years"), 
                                                                              new Choice(">= 2 years", "More than 2 years")),
                                                     new RadioButtonsQuestion("experienceWithSED",
                                                                              "Experience with SED", 
                                                                              true,
                                                                              null, 
                                                                              new NotUndefinedValueValidator("Experience with SED not defined."), 
                                                                              false,
                                                                              new Choice("None", "None"), 
                                                                              new Choice("< 1 year", "Less than 1 year"), 
                                                                              new Choice(">= 1 year", "More than 1 year")));
      SendFormPage sendConditionsPage = new SendFormPage("sendConditions", 
                                                         "Confirm Sending Background Knowledge (used to order proof attempts)", 
                                                         "Optionally, inspect the answers to be sent.", 
                                                         "Current date and time (nothing else!)");
      FixedForm introductionForm = new FixedForm(INTRODUCTION_FORM_NAME, 
                                                 false,
                                                 RANDOM_COMPUTER_NAME,
                                                 conditionsPage, 
                                                 backgroundPage,
                                                 sendConditionsPage);
      // Create evaluation form
      URL evaluationURL = isUIAvailable() ? toLocalURL("data/reviewingCode/instructions/EvaluationIntroduction-Screencast.html") : null;
      URL jmlURL = isUIAvailable() ? toLocalURL("data/reviewingCode/instructions/JML.html") : null;
      InstructionPage evaluationPage = new InstructionPage(EVALUATION_PAGE_NAME, "Evaluation Instructions", "Read the evaluation instructions carefully before continuing.", evaluationURL, isUIAvailable() ? EvaluationModelImages.getImage(EvaluationModelImages.EVALUATION) : null);
      InstructionPage jmlPage = new InstructionPage(JML_PAGE_NAME, "JML", "Read the JML introduction carefully before continuing.", jmlURL, isUIAvailable() ? EvaluationModelImages.getImage(EvaluationModelImages.JML_LOGO) : null);
      ToolPage noToolPage = new ToolPage(getTool(NO_TOOL_NAME),
                                         null,
                                         false);
            
      ToolPage sedToolPage = new ToolPage(getTool(SED_TOOL_NAME),
                                          null, // TODO: Provide example
                                          false);
      QuestionPage example1Page = createObservableArrayQuestionPage(EXAMPLE_1_PAGE_NAME, "Review of cass ObservableArray");
      QuestionPage example2Page = createBankUtilQuestionPage(EXAMPLE_2_PAGE_NAME, "Review of class BankUtil");
      QuestionPage example3Page = createIntegerUtilQuestionPage(EXAMPLE_3_PAGE_NAME, "Review of class IntegerUtil");
      QuestionPage example4Page = createMathUtilQuestionPage(EXAMPLE_4_PAGE_NAME, "Review of class MathUtil");
      QuestionPage example5Page = createValueSearchQuestionPage(EXAMPLE_5_PAGE_NAME, "Review of class ValueSearch");
      QuestionPage example6Page = createStackQuestionPage(EXAMPLE_6_PAGE_NAME, "Review of class Stack");
      QuestionPage feedbackPage = createFeedbackPage();
      SendFormPage sendEvaluationPage = new SendFormPage(SEND_EVALUATION_PAGE_NAME, 
                                                         "Confirm Sending Evaluation Answers", 
                                                         "Optionally, inspect the answers to be sent.", 
                                                         "Current date and time (nothing else!)");
      RandomForm evaluationForm = new RandomForm(EVALUATION_FORM_NAME, true, evaluationPage, jmlPage, noToolPage, sedToolPage, example1Page, example2Page, example3Page, example4Page, example5Page, example6Page, feedbackPage, sendEvaluationPage);
      // Create thanks form
      QuestionPage thanksPage = new QuestionPage("thanksPage", 
                                                 "Evaluation sucessfully completed", 
                                                 "Thank you for participating in the evaluation.", 
                                                 false, 
                                                 false,
                                                 null,
                                                 new ImageQuestion("thanksImage", isUIAvailable() ? EvaluationModelImages.getImage(EvaluationModelImages.KEY_THANKS, 25) : null));
      FixedForm thanksForm = new FixedForm("thanksForm", false, thanksPage);
      return CollectionUtil.toList(introductionForm, evaluationForm, thanksForm);
   }

   
   
   
   
   
   
   private QuestionPage createFeedbackPage() {
      List<Choice> choices = CollectionUtil.toList(new Choice("Very Helpful", "Very Helpful"), 
                                                   new Choice("Helpful", "Helpful"), 
                                                   new Choice("Little Helpful", "Little Helpful"), 
                                                   new Choice("Not Helpful", "Not Helpful"), 
                                                   new Choice("Never Used", "Never Used"));
      // SED
      String setTitle = "Shown symbolic execution tree";
      RadioButtonsQuestion setQuestion = new RadioButtonsQuestion("set", 
                                                                  setTitle, 
                                                                  isUIAvailable() ? EvaluationModelImages.getImage(EvaluationModelImages.SED_SET) : null,
                                                                  false,
                                                                  null, 
                                                                  new NotUndefinedValueValidator("Question '" + setTitle + "' not answered."), 
                                                                  false,
                                                                  choices);
      String reachedTitle = "Highlighting of source code reached during symbolic execution";
      RadioButtonsQuestion reachedQuestion = new RadioButtonsQuestion("reachedSourceCode", 
                                                                      reachedTitle, 
                                                                      isUIAvailable() ? EvaluationModelImages.getImage(EvaluationModelImages.SED_REACHED) : null,
                                                                      false,
                                                                      null, 
                                                                      new NotUndefinedValueValidator("Question '" + reachedTitle + "' not answered."), 
                                                                      false,
                                                                      choices);
      String variablesTitle = "Shown variables of a node (view 'Variables')";
      RadioButtonsQuestion variablesQuestion = new RadioButtonsQuestion("variables", 
                                                                        variablesTitle, 
                                                                        isUIAvailable() ? EvaluationModelImages.getImage(EvaluationModelImages.SED_VARIABLES) : null,
                                                                        false,
                                                                        null, 
                                                                        new NotUndefinedValueValidator("Question '" + variablesTitle + "' not answered."), 
                                                                        false,
                                                                        choices);
      String layoutTitle = "Visualization of memory layouts";
      RadioButtonsQuestion layoutQuestion = new RadioButtonsQuestion("layouts", 
                                                                     layoutTitle, 
                                                                     isUIAvailable() ? EvaluationModelImages.getImage(EvaluationModelImages.SED_MEMORY_LAYOUTS) : null,
                                                                     false,
                                                                     null, 
                                                                     new NotUndefinedValueValidator("Question '" + layoutTitle + "' not answered."), 
                                                                     false,
                                                                     choices);
      SectionQuestion sedSection = new SectionQuestion("SED", "SED", false, setQuestion, reachedQuestion, variablesQuestion, layoutQuestion);
      // NO_TOOL vs SED
      String keyVsSedTitle = "I prefer to inspect source code";
      RadioButtonsQuestion keyVsSedQuestion = new RadioButtonsQuestion("toolPreference", 
                                                                       keyVsSedTitle, 
                                                                       true,
                                                                       null, 
                                                                       new NotUndefinedValueValidator("Question '" + keyVsSedTitle + "' not answered."), 
                                                                       false,
                                                                       new Choice("directly", "Directly"),
                                                                       new Choice("directly and using SED, both are equally good", "DirectlyAndSEDequal"),
                                                                       new Choice("directly and using SED, depending on the proof", "DirectlyAndSEDproof"),
                                                                       new Choice("directly and using SED, both are equally bad and should be improved", "DirectlyAndSEDbad"),
                                                                       new Choice("using SED", "SED"));
      SectionQuestion keyVsSedSection = new SectionQuestion("KeYvsSED", "KeY vs SED", false, keyVsSedQuestion);
      // Feedback
      SectionQuestion feedbackSection = new SectionQuestion("feedback", 
                                                            "Feedback", 
                                                            true, 
                                                            new TextQuestion("feedback", "Feedback about the tools or the evaluation (optional)", null, null, false));
      return new QuestionPage(FEEDBACK_PAGE,
                              "Feedback", 
                              "Please answer the question to give us some feeback about the tools and the evaluation.", 
                              false,
                              false,
                              null,
                              sedSection,
                              keyVsSedSection,
                              feedbackSection);
   }
   
   
   
   

   
   private QuestionPage createValueSearchQuestionPage(String pageName, String title) {
      String description = "find(int[], int) related question.";
      String methodProblemsTitle = "What is wrong?";
      CheckboxQuestion methodProblems = new CheckboxQuestion("methodProblems", 
                                                             methodProblemsTitle, 
                                                             description,
                                                             true, 
                                                             null, 
                                                             createNotUndefinedValueValidator(methodProblemsTitle), 
                                                             true,
                                                             new Choice("All array elements are considered during search", "AllConsidered"), 
                                                             new Choice("Not all array elements are considered during search", "NotAllConsidered"), 
                                                             new Choice("First matching array element is returned", "FirstFoundReturned"), 
                                                             new Choice("Not the first matching array element is returned", "NotFirstFoundReturned"), 
                                                             new Choice("-1 is returned instead of found index", "MinusOneReturned"), 
                                                             new Choice("Found index is retruned instead of -1", "FoundReturned"), 
                                                             new Choice("Index of wrong array element might be returned", "WrongIndex", true), 
                                                             new Choice("array is modified during search", "ArrayModified"), 
                                                             new Choice("array is not modified during search", "ArrayNotModified"), 
                                                             createElseWrongChoice(description));
      String implementedAsDocumentedTitle = "Does the method implementation operates as specified by its JavaDoc comment?";
      RadioButtonsQuestion implementedAsDocumented = new RadioButtonsQuestion("implementedAsDocumented", 
                                                                              implementedAsDocumentedTitle, 
                                                                              description,
                                                                              true, 
                                                                              null, 
                                                                              createNotUndefinedValueValidator(implementedAsDocumentedTitle), 
                                                                              true,
                                                                              new Choice("Yes", "Yes"), 
                                                                              new Choice("No", "No", true, methodProblems));
      String executedTitle = createExecutedQuestion("find(int[], int)");
      CheckboxQuestion executedQuestion = new CheckboxQuestion("executedStatements", 
                                                               executedTitle, 
                                                               description,
                                                               true,
                                                               null, 
                                                               new NotUndefinedValueValidator("Question '" + executedTitle + "' not answered."), 
                                                               true,
                                                               new Choice("None of the statements can be executed", "None"),
                                                               new Choice("Line 20: return new ValueSearch().search(array)", "Line 20", true),
                                                               new Choice("Line 31: if (index < 0 || index >= array.length)", "Line 31", true),
                                                               new Choice("Line 32: return false", "Line 32"),
                                                               new Choice("Line 35: return array[index] == value", "Line 35", true));
      return new QuestionPage(pageName, 
                              title, 
                              createQuestionPageMessage(), 
                              true,
                              true,
                              null,
                              new LabelQuestion("generalDescription", createGeneralClassDescription("ValueSearch")),
                              implementedAsDocumented,
                              createThrownExceptionsQuestion(description, true, false, false, false, false, false),
                              executedQuestion,
                              createSEDUsedQuestion(),
                              createCodeExecutedQuestion());
   }

   
   
   
   

   
   private QuestionPage createBankUtilQuestionPage(String pageName, String title) {
      String description = "computeInsuranceRate(int) related question.";
      String methodProblemsTitle = "What is wrong?";
      CheckboxQuestion methodProblems = new CheckboxQuestion("methodProblems", 
                                                             methodProblemsTitle, 
                                                             description,
                                                             true, 
                                                             null, 
                                                             createNotUndefinedValueValidator(methodProblemsTitle), 
                                                             true,
                                                             new Choice("Wrong value returned in case age < 18", "WrongLess18", createBankUtilReturnedValue(description, false)), 
                                                             new Choice("Wrong value returned in case age >= 18 and age < 19", "WrongLess19", createBankUtilReturnedValue(description, false)), 
                                                             new Choice("Wrong value returned in case age >= 19 and age < 21", "WrongLess21", createBankUtilReturnedValue(description, false)), 
                                                             new Choice("Wrong value returned in case age >= 21 and age < 35", "WrongLess35", createBankUtilReturnedValue(description, false)), 
                                                             new Choice("Wrong value returned in case age >= 35", "WrongGreaterOrEqual35", true, createBankUtilReturnedValue(description, true)), 
                                                             createElseWrongChoice(description));
      String implementedAsDocumentedTitle = "Does the method implementation operates as specified by its JavaDoc comment?";
      RadioButtonsQuestion implementedAsDocumented = new RadioButtonsQuestion("implementedAsDocumented", 
                                                                              implementedAsDocumentedTitle, 
                                                                              description,
                                                                              true, 
                                                                              null, 
                                                                              createNotUndefinedValueValidator(implementedAsDocumentedTitle), 
                                                                              true,
                                                                              new Choice("Yes", "Yes"), 
                                                                              new Choice("No", "No", true, methodProblems));
      String executedTitle = createExecutedQuestion("computeInsuranceRate(int)");
      CheckboxQuestion executedQuestion = new CheckboxQuestion("executedStatements", 
                                                               executedTitle, 
                                                               description,
                                                               true,
                                                               null, 
                                                               new NotUndefinedValueValidator("Question '" + executedTitle + "' not answered."), 
                                                               true,
                                                               new Choice("None of the statements can be executed", "None"),
                                                               new Choice("Line 18: int[] ageLimits = {18, 19, 21, 35, 65}", "Line 18", true),
                                                               new Choice("Line 19: long[] insuranceRates = {200, 250, 300, 450, 575}", "Line 19", true),
                                                               new Choice("Line 20: int ageLevel = 0", "Line 20", true),
                                                               new Choice("Line 21: long insuranceRate = 570", "Line 21", true),
                                                               new Choice("Line 22: while (ageLevel < ageLimits.length - 1)", "Line 22", true),
                                                               new Choice("Line 23: if (age < ageLimits[ageLevel])", "Line 23", true),
                                                               new Choice("Line 24: return insuranceRates[ageLevel]", "Line 24", true),
                                                               new Choice("Line 26: ageLevel++", "Line 26", true),
                                                               new Choice("Line 28: return insuranceRate", "Line 28", true));
      return new QuestionPage(pageName, 
                              title, 
                              createQuestionPageMessage(), 
                              true,
                              true,
                              null,
                              new LabelQuestion("generalDescription", createGeneralClassDescription("BankUtil")),
                              implementedAsDocumented,
                              createThrownExceptionsQuestion(description, false, false, false, false, false, false),
                              executedQuestion,
                              createSEDUsedQuestion(),
                              createCodeExecutedQuestion());
   }

   private CheckboxQuestion createBankUtilReturnedValue(String description, boolean expectedSelected) {
      String returnedValueTitle = "Which value is returned?";
      return new CheckboxQuestion("methodProblems", 
                                  returnedValueTitle, 
                                  description,
                                  true, 
                                  null, 
                                  createNotUndefinedValueValidator(returnedValueTitle), 
                                  true,
                                  new Choice("-1", "-1"), 
                                  new Choice("0", "0"), 
                                  new Choice("18", "18"), 
                                  new Choice("19", "19"), 
                                  new Choice("21", "21"), 
                                  new Choice("35", "35"), 
                                  new Choice("65", "65"), 
                                  new Choice("200", "200"), 
                                  new Choice("250", "250"), 
                                  new Choice("300", "300"), 
                                  new Choice("450", "450"), 
                                  new Choice("570", "570", expectedSelected), 
                                  new Choice("575", "575"), 
                                  createElseRetrunedChoice(description));

   }

   
   
   
   

   
   private QuestionPage createMathUtilQuestionPage(String pageName, String title) {
      String description = "median(int[], int, int) related question.";
      String methodProblemsTitle = "What is wrong?";
      CheckboxQuestion methodProblems = new CheckboxQuestion("methodProblems", 
                                                             methodProblemsTitle, 
                                                             description,
                                                             true, 
                                                             null, 
                                                             createNotUndefinedValueValidator(methodProblemsTitle), 
                                                             true,
                                                             new Choice("The returned value is contained in the array", "ContainedInArray"), 
                                                             new Choice("The returned value is not contained in the array", "NotContainedInArray"), 
                                                             new Choice("Middle value returned instead of average", "MiddleInsteadOfAverage"), 
                                                             new Choice("Average returned instead of middle value", "AverageInsteadOfMiddle"), 
                                                             new Choice("Average is computed wrongly", "WrongAverage"), 
                                                             new Choice("array is modified during compuation", "ArrayModified"), 
                                                             new Choice("array is not modified during compuation", "ArrayNotModified"), 
                                                             new Choice("Exception is thrown instead of returning average", "ExceptionThrown"), 
                                                             new Choice("Value is returned instead of thrown exception", "ValueReturned"), 
                                                             createElseWrongChoice(description));
      String implementedAsDocumentedTitle = "Does the method implementation operates as specified by its JavaDoc comment?";
      RadioButtonsQuestion implementedAsDocumented = new RadioButtonsQuestion("implementedAsDocumented", 
                                                                              implementedAsDocumentedTitle, 
                                                                              description,
                                                                              true, 
                                                                              null, 
                                                                              createNotUndefinedValueValidator(implementedAsDocumentedTitle), 
                                                                              true,
                                                                              new Choice("Yes", "Yes", true), 
                                                                              new Choice("No", "No", methodProblems));
      String executedTitle = createExecutedQuestion("median(int[], int, int)");
      CheckboxQuestion executedQuestion = new CheckboxQuestion("executedStatements", 
                                                               executedTitle, 
                                                               description,
                                                               true,
                                                               null, 
                                                               new NotUndefinedValueValidator("Question '" + executedTitle + "' not answered."), 
                                                               true,
                                                               new Choice("None of the statements can be executed", "None"),
                                                               new Choice("Line 22: if (array == null)", "Line 22", true),
                                                               new Choice("Line 23: throw new IllegalArgumentException(\"Array is null.\")", "Line 23", true),
                                                               new Choice("Line 25: if (start < 0 || start >= array.length)", "Line 25", true),
                                                               new Choice("Line 26: throw new IllegalArgumentException(\"Start is not a valid array index.\")", "Line 26", true),
                                                               new Choice("Line 28: if (end < 0 || end >= array.length)", "Line 28", true),
                                                               new Choice("Line 29: throw new IllegalArgumentException(\"Start is not a valid array index.\")", "Line 29", true),
                                                               new Choice("Line 32: int middle = (start + end) / 2", "Line 32", true),
                                                               new Choice("Line 33:  if ((start + end) % 2 == 0)", "Line 33", true),
                                                               new Choice("Line 34: return array[middle]", "Line 34", true),
                                                               new Choice("Line 37: return (array[middle] + array[middle + 1]) / 2", "Line 37", true));
      return new QuestionPage(pageName, 
                              title, 
                              createQuestionPageMessage(), 
                              true,
                              true,
                              null,
                              new LabelQuestion("generalDescription", createGeneralClassDescription("MathUtil")),
                              implementedAsDocumented,
                              createThrownExceptionsQuestion(description, false, false, false, false, true, false),
                              executedQuestion,
                              createSEDUsedQuestion(),
                              createCodeExecutedQuestion());
   }

   
   
   
   

   
   private QuestionPage createIntegerUtilQuestionPage(String pageName, String title) {
      String description = "middle(int, int, int) related question.";
      String methodProblemsTitle = "What is wrong?";
      CheckboxQuestion methodProblems = new CheckboxQuestion("methodProblems", 
                                                             methodProblemsTitle, 
                                                             description,
                                                             true, 
                                                             null, 
                                                             createNotUndefinedValueValidator(methodProblemsTitle), 
                                                             true,
                                                             new Choice("The returned value is x, y or z", "XYZReturned"), 
                                                             new Choice("The returned value is none of x, y and z", "NotXYZReturned"), 
                                                             new Choice("x returned instead of y", "xInsteadOfy"), 
                                                             new Choice("x returned instead of z", "xInsteadOfz"), 
                                                             new Choice("y returned instead of x", "yInsteadOfx", true), 
                                                             new Choice("y returned instead of z", "yInsteadOfz"), 
                                                             new Choice("z returned instead of x", "zInsteadOfx"), 
                                                             new Choice("z returned instead of y", "zInsteadOfy"), 
                                                             createElseWrongChoice(description));
      String implementedAsDocumentedTitle = "Does the method implementation operates as specified by its JavaDoc comment?";
      RadioButtonsQuestion implementedAsDocumented = new RadioButtonsQuestion("implementedAsDocumented", 
                                                                              implementedAsDocumentedTitle, 
                                                                              description,
                                                                              true, 
                                                                              null, 
                                                                              createNotUndefinedValueValidator(implementedAsDocumentedTitle), 
                                                                              true,
                                                                              new Choice("Yes", "Yes"), 
                                                                              new Choice("No", "No", true, methodProblems));
      String executedTitle = createExecutedQuestion("middle(int, int, int)");
      CheckboxQuestion executedQuestion = new CheckboxQuestion("executedStatements", 
                                                               executedTitle, 
                                                               description,
                                                               true,
                                                               null, 
                                                               new NotUndefinedValueValidator("Question '" + executedTitle + "' not answered."), 
                                                               true,
                                                               new Choice("None of the statements can be executed", "None"),
                                                               new Choice("Line 13: if (y < z)", "Line 13", true),
                                                               new Choice("Line 14: if (x < y)", "Line 14", true),
                                                               new Choice("Line 15: return y", "Line 15", true),
                                                               new Choice("Line 17: if (x < z)", "Line 17", true),
                                                               new Choice("Line 18: return y", "Line 18", true),
                                                               new Choice("Line 22: if (x > y)", "Line 22", true),
                                                               new Choice("Line 23: return y", "Line 23", true),
                                                               new Choice("Line 25: if (x > z)", "Line 25", true),
                                                               new Choice("Line 26: return x", "Line 26", true),
                                                               new Choice("Line 29: return z", "Line 29", true));
      return new QuestionPage(pageName, 
                              title, 
                              createQuestionPageMessage(), 
                              true,
                              true,
                              null,
                              new LabelQuestion("generalDescription", createGeneralClassDescription("IntegerUtil")),
                              implementedAsDocumented,
                              createThrownExceptionsQuestion(description, false, false, false, false, false, false),
                              executedQuestion,
                              createSEDUsedQuestion(),
                              createCodeExecutedQuestion());
   }
   
   
   
   
   
   
   
   
   
   
   private QuestionPage createObservableArrayQuestionPage(String pageName, String title) {
      TabbedQuestion tabbedQuestion = new TabbedQuestion("methods", 
                                                         createObservableArrayArrayTab(),
                                                         createSetTab(),
                                                         createSetArrayListenersTab());
      return new QuestionPage(pageName, 
                              title, 
                              createQuestionPageMessage(), 
                              false,
                              false,
                              true,
                              null,
                              new LabelQuestion("generalDescription", createGeneralClassDescription("ObservableArray")),
                              tabbedQuestion);
   }
   
   private TabQuestion createObservableArrayArrayTab() {
      String description = "ObservableArray(Object[]) related question.";
      String methodProblemsTitle = "What is wrong?";
      CheckboxQuestion methodProblems = new CheckboxQuestion("methodProblems", 
                                                             methodProblemsTitle, 
                                                             description,
                                                             true, 
                                                             null, 
                                                             createNotUndefinedValueValidator(methodProblemsTitle), 
                                                             true,
                                                             new Choice("Future calls of set(int, Object) will modify the given array", "ArrayModified"), 
                                                             new Choice("Future calls of set(int, Object) will not modify the given array", "ArrayNotModified"), 
                                                             new Choice("ObservableArray is created instead of throwing an exception", "ExceptionMissing"), 
                                                             new Choice("Exception is thrown instead of creating an ObservableArray", "ExceptionThrown"), 
                                                             createElseWrongChoice(description));
      String implementedAsDocumentedTitle = "Does the constructor implementation operates as specified by its JavaDoc comment?";
      RadioButtonsQuestion implementedAsDocumented = new RadioButtonsQuestion("implementedAsDocumented", 
                                                                              implementedAsDocumentedTitle, 
                                                                              description,
                                                                              true, 
                                                                              null, 
                                                                              createNotUndefinedValueValidator(implementedAsDocumentedTitle), 
                                                                              true,
                                                                              new Choice("Yes", "Yes", true), 
                                                                              new Choice("No", "No", methodProblems));
      String executedTitle = createExecutedQuestion("ObservableArray(Object[])");
      CheckboxQuestion executedQuestion = new CheckboxQuestion("executedStatements", 
                                                               executedTitle, 
                                                               description,
                                                               true,
                                                               null, 
                                                               new NotUndefinedValueValidator("Question '" + executedTitle + "' not answered."), 
                                                               true,
                                                               new Choice("None of the statements can be executed", "None"),
                                                               new Choice("Line 30: if (array == null)", "Line 30", true),
                                                               new Choice("Line 31: throw new IllegalArgumentException(\"Array is null.\")", "Line 31", true),
                                                               new Choice("Line 33: this.array = array", "Line 33", true),
                                                               new Choice("Line 34: this.arrayListeners = null", "Line 34", true));
      return new TabQuestion("ObservableArray", 
                             "ObservableArray(Object[])", 
                             false, 
                             implementedAsDocumented,
                             createObservableArrayClassInvariantQuestion(description, true),
                             createThrownExceptionsQuestion(description, false, false, false, true, false, false),
                             executedQuestion,
                             createObservableArrayLocationQuestion(description, true, true, false),
                             createSEDUsedQuestion(),
                             createCodeExecutedQuestion());
   }
   
   private TabQuestion createSetTab() {
      String description = "set(int, Object) related question.";
      String methodProblemsTitle = "What is wrong?";
      CheckboxQuestion methodProblems = new CheckboxQuestion("methodProblems", 
                                                             methodProblemsTitle, 
                                                             description,
                                                             true, 
                                                             null, 
                                                             createNotUndefinedValueValidator(methodProblemsTitle), 
                                                             true,
                                                             new Choice("array[index] is assigned to element", "ArrayUpdated"), 
                                                             new Choice("array[index] is not assigned to element", "ArrayNotUpdated"), 
                                                             new Choice("All ArrayListener of arrayListeners are informed about the change", "ArrayListenerInformed"), 
                                                             new Choice("Not all ArrayListener of arrayListeners are informed about the change", "ArrayListenerNotInformed"), 
                                                             new Choice("An ArrayEvent is created.", "ArrayEventCreated"), 
                                                             new Choice("No ArrayEvent is created.", "ArrayEventNotCreated"), 
                                                             new Choice("The ArrayEvent contains all details about the modification.", "ArrayEventHasDetails"), 
                                                             new Choice("The ArrayEvent does not contains all details about the modification.", "ArrayEventDoesNotHaveDetails"), 
                                                             createElseWrongChoice(description));
      String implementedAsDocumentedTitle = "Does the constructor implementation operates as specified by its JavaDoc comment?";
      RadioButtonsQuestion implementedAsDocumented = new RadioButtonsQuestion("implementedAsDocumented", 
                                                                              implementedAsDocumentedTitle, 
                                                                              description,
                                                                              true, 
                                                                              null, 
                                                                              createNotUndefinedValueValidator(implementedAsDocumentedTitle), 
                                                                              true,
                                                                              new Choice("Yes", "Yes", true), 
                                                                              new Choice("No", "No", methodProblems));
      String executedTitle = createExecutedQuestion("set(int, Object)");
      CheckboxQuestion executedQuestion = new CheckboxQuestion("executedStatements", 
                                                               executedTitle, 
                                                               description,
                                                               true,
                                                               null, 
                                                               new NotUndefinedValueValidator("Question '" + executedTitle + "' not answered."), 
                                                               true,
                                                               new Choice("None of the statements can be executed", "None"),
                                                               new Choice("Line 49: array[index] = element", "Line 49", true),
                                                               new Choice("Line 50: fireElementChanged(new ArrayEvent(this, index, element))", "Line 50", true),
                                                               new Choice("Line 59: if (arrayListeners != null)", "Line 59", true),
                                                               new Choice("Line 64: int i = 0", "Line 64 initial", true),
                                                               new Choice("Line 64: i < arrayListeners.length", "Line 64 guard", true),
                                                               new Choice("Line 64: i++", "Line 64 increment", true),
                                                               new Choice("Line 65 if (arrayListeners[i] != null)", "Line 65", true),
                                                               new Choice("Line 66 arrayListeners[i].elementChanged(e)", "Line 66", true));
      return new TabQuestion("set", 
                             "set(int, Object)", 
                             false, 
                             implementedAsDocumented,
                             createObservableArrayClassInvariantQuestion(description, false),
                             createThrownExceptionsQuestion(description, true, false, false, false, true, true),
                             executedQuestion,
                             createObservableArrayLocationQuestion(description, false, false, true),
                             createSEDUsedQuestion(),
                             createCodeExecutedQuestion());
   }
   
   private TabQuestion createSetArrayListenersTab() {
      String description = "setArrayListeners(ArrayListener[]) related question.";
      String methodProblemsTitle = "What is wrong?";
      CheckboxQuestion methodProblems = new CheckboxQuestion("methodProblems", 
                                                             methodProblemsTitle, 
                                                             description,
                                                             true, 
                                                             null, 
                                                             createNotUndefinedValueValidator(methodProblemsTitle), 
                                                             true,
                                                             new Choice("ArrayListener are replaced by the new once", "ArrayListenerReplaced"), 
                                                             new Choice("ArrayListener are not replaced by the new once", "ArrayListenerNotReplaced"), 
                                                             createElseWrongChoice(description));
      String implementedAsDocumentedTitle = "Does the constructor implementation operates as specified by its JavaDoc comment?";
      RadioButtonsQuestion implementedAsDocumented = new RadioButtonsQuestion("implementedAsDocumented", 
                                                                              implementedAsDocumentedTitle, 
                                                                              description,
                                                                              true, 
                                                                              null, 
                                                                              createNotUndefinedValueValidator(implementedAsDocumentedTitle), 
                                                                              true,
                                                                              new Choice("Yes", "Yes", true), 
                                                                              new Choice("No", "No", methodProblems));
      String executedTitle = createExecutedQuestion("setArrayListeners(ArrayListener[])");
      CheckboxQuestion executedQuestion = new CheckboxQuestion("executedStatements", 
                                                               executedTitle, 
                                                               description,
                                                               true,
                                                               null, 
                                                               new NotUndefinedValueValidator("Question '" + executedTitle + "' not answered."), 
                                                               true,
                                                               new Choice("None of the statements can be executed", "None"),
                                                               new Choice("Line 74 this.arrayListeners = arrayListeners", "Line 74", true));
      return new TabQuestion("setArrayListeners", 
                             "setArrayListeners(ArrayListener[])", 
                             false, 
                             implementedAsDocumented,
                             createObservableArrayClassInvariantQuestion(description, false),
                             createThrownExceptionsQuestion(description, false, false, false, false, false, false),
                             executedQuestion,
                             createObservableArrayLocationQuestion(description, false, true, false),
                             createSEDUsedQuestion(),
                             createCodeExecutedQuestion());
   }
   
   private RadioButtonsQuestion createObservableArrayClassInvariantQuestion(String description, boolean constructor) {
      String problemsTitle = "What is wrong?";
      CheckboxQuestion problems = new CheckboxQuestion("classInvariantProblems", 
                                                       problemsTitle, 
                                                       description,
                                                       true, 
                                                       null, 
                                                       createNotUndefinedValueValidator(problemsTitle), 
                                                       true,
                                                       new Choice("array might be null", "ArrayNull"), 
                                                       new Choice("array might be not null", "ArrayNotNull"), 
                                                       new Choice("array might have length 0", "ArrayLengthZero"), 
                                                       new Choice("array might be empty", "ArrayEmpty"), 
                                                       new Choice("array might be not empty", "ArrayNotEmpty"), 
                                                       new Choice("array might contain null as element", "ArrayContainsNull"), 
                                                       new Choice("array might contain an Object as element", "ArrayContainsObject"), 
                                                       new Choice("arrayListeners might be null", "ArrayListenersNull"), 
                                                       new Choice("arrayListeners might be not null", "ArrayListenersNotNull"), 
                                                       new Choice("arrayListeners might have length 0", "ArrayListenersLengthZero"), 
                                                       new Choice("arrayListeners might be empty", "ArrayListenersEmpty"), 
                                                       new Choice("arrayListeners might be not empty", "ArrayListenersNotEmpty"), 
                                                       new Choice("arrayListeners might contain null as element", "ArrayListenersContainsNull"), 
                                                       new Choice("arrayListeners might contain an Object as element", "ArrayarrayListenersContainsObject"), 
                                                       createElseWrongChoice(description));
      String title = constructor ?
                     "Is the class invariant established?" :
                     "Is the class invariant preserved?";
      return new RadioButtonsQuestion("classInvariant", 
                                      title, 
                                      description,
                                      true, 
                                      null, 
                                      createNotUndefinedValueValidator(title), 
                                      true,
                                      new Choice("Yes", "Yes", true), 
                                      new Choice("No", "No", problems));
   }

   private CheckboxQuestion createObservableArrayLocationQuestion(String description, boolean expectedArray, boolean expectedArrayListeners, boolean expectedArrayAtIndex) {
      String title = "Which location(s) of the initial state before method invocation might be changed during execution?";
      return new CheckboxQuestion("changedLocations", 
                                  title, 
                                  description,
                                  true,
                                  null, 
                                  new NotUndefinedValueValidator("Question '" + title + "' not answered."), 
                                  true,
                                  new Choice("None", "None"),
                                  new Choice("array", "array", expectedArray),
                                  new Choice("array[index - 1]", "array[index - 1]"),
                                  new Choice("array[index]", "array[index]", expectedArrayAtIndex),
                                  new Choice("array[index + 1]", "array[index + 1]"),
                                  new Choice("array[*]", "array[*]"),
                                  new Choice("array.length", "array.length"),
                                  new Choice("arrayListeners", "arrayListeners", expectedArrayListeners),
                                  new Choice("arrayListeners[i - 1]", "arrayListeners[i - 1]"),
                                  new Choice("arrayListeners[i]", "arrayListeners[i]"),
                                  new Choice("arrayListeners[i + 1]", "arrayListeners[i + 1]"),
                                  new Choice("arrayListeners[*]", "arrayListeners[*]"),
                                  new Choice("arrayListeners.length", "arrayListeners.length"),
                                  new Choice("something else", "SomethingElse", createElseExceptionSubQuestion(description)));
   }
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   

   private QuestionPage createStackQuestionPage(String pageName, String title) {
      TabbedQuestion tabbedQuestion = new TabbedQuestion("methods", 
                                                         createStackIntTab(),
                                                         createStackStackTab(),
                                                         createPushTab(),
                                                         createPopTab());
      return new QuestionPage(pageName, 
                              title, 
                              createQuestionPageMessage(), 
                              false,
                              false,
                              true,
                              null,
                              new LabelQuestion("generalDescription", createGeneralClassDescription("Stack")),
                              tabbedQuestion);
   }
   
   private TabQuestion createStackIntTab() {
      String description = "Stack(int) related question.";
      String methodProblemsTitle = "What is wrong?";
      CheckboxQuestion methodProblems = new CheckboxQuestion("methodProblems", 
                                                             methodProblemsTitle, 
                                                             description,
                                                             true, 
                                                             null, 
                                                             createNotUndefinedValueValidator(methodProblemsTitle), 
                                                             true,
                                                             new Choice("The created stack is empty", "StackEmpty"), 
                                                             new Choice("The created stack is not empty", "StackNotEmpty"), 
                                                             new Choice("The created stack can be filled up to maximal size", "StackSizeMaximal"),
                                                             new Choice("The created stack can not be filled up to maximal size", "StackSizeNotMaximal"),
                                                             createElseWrongChoice(description));
      String implementedAsDocumentedTitle = "Does the constructor implementation operates as specified by its JavaDoc comment?";
      RadioButtonsQuestion implementedAsDocumented = new RadioButtonsQuestion("implementedAsDocumented", 
                                                                              implementedAsDocumentedTitle, 
                                                                              description,
                                                                              true, 
                                                                              null, 
                                                                              createNotUndefinedValueValidator(implementedAsDocumentedTitle), 
                                                                              true,
                                                                              new Choice("Yes", "Yes", true), 
                                                                              new Choice("No", "No", methodProblems));
      String executedTitle = createExecutedQuestion("Stack(int)");
      CheckboxQuestion executedQuestion = new CheckboxQuestion("executedStatements", 
                                                               executedTitle, 
                                                               description,
                                                               true,
                                                               null, 
                                                               new NotUndefinedValueValidator("Question '" + executedTitle + "' not answered."), 
                                                               true,
                                                               new Choice("None of the statements can be executed", "None"),
                                                               new Choice("Line 33: elements = new Object[maximalSize]", "Line 33", true),
                                                               new Choice("Line 34: size = 0", "Line 34", true));
      return new TabQuestion("Stack_int", 
                             "Stack(int)", 
                             false, 
                             implementedAsDocumented,
                             createStackClassInvariantQuestion(description, true, false),
                             createThrownExceptionsQuestion(description, false, true, false, false, false, false),
                             executedQuestion,
                             createStackLocationQuestion(description, true, true, false),
                             createSEDUsedQuestion(),
                             createCodeExecutedQuestion());
   }
   
   private TabQuestion createStackStackTab() {
      String description = "Stack(Stack) related question.";
      String methodProblemsTitle = "What is wrong?";
      CheckboxQuestion methodProblems = new CheckboxQuestion("methodProblems", 
                                                             methodProblemsTitle, 
                                                             description,
                                                             true, 
                                                             null, 
                                                             createNotUndefinedValueValidator(methodProblemsTitle), 
                                                             true,
                                                             new Choice("The created stack is empty", "StackEmpty"), 
                                                             new Choice("The created stack is not empty", "StackNotEmpty"), 
                                                             new Choice("The created stack provides same content as the existing once", "SameContent"),
                                                             new Choice("The created stack provides different content as the existing once", "DifferentContent"),
                                                             new Choice("The created stack has same size as the existing once", "SameSize"),
                                                             new Choice("The created stack has different size as the existing once", "DifferentSize"),
                                                             new Choice("The created stack has same elements array as the existing once", "SameElements", true),
                                                             new Choice("The created stack has different elements array as the existing once", "DifferentElements"),
                                                             createElseWrongChoice(description));
      String implementedAsDocumentedTitle = "Does the constructor implementation operates as specified by its JavaDoc comment?";
      RadioButtonsQuestion implementedAsDocumented = new RadioButtonsQuestion("implementedAsDocumented", 
                                                                              implementedAsDocumentedTitle, 
                                                                              description,
                                                                              true, 
                                                                              null, 
                                                                              createNotUndefinedValueValidator(implementedAsDocumentedTitle), 
                                                                              true,
                                                                              new Choice("Yes", "Yes"), 
                                                                              new Choice("No", "No", true, methodProblems));
      String executedTitle = createExecutedQuestion("Stack(Stack)");
      CheckboxQuestion executedQuestion = new CheckboxQuestion("executedStatements", 
                                                               executedTitle, 
                                                               description,
                                                               true,
                                                               null, 
                                                               new NotUndefinedValueValidator("Question '" + executedTitle + "' not answered."), 
                                                               true,
                                                               new Choice("None of the statements can be executed", "None"),
                                                               new Choice("Line 43: this.elements = existingStack.elements", "Line 43", true),
                                                               new Choice("Line 44: this.size = existingStack.size", "Line 44", true));
      return new TabQuestion("Stack_Stack", 
                             "Stack(Stack)", 
                             false, 
                             implementedAsDocumented,
                             createStackClassInvariantQuestion(description, true, false),
                             createThrownExceptionsQuestion(description, true, false, false, false, false, false),
                             executedQuestion,
                             createStackLocationQuestion(description, true, true, false),
                             createSEDUsedQuestion(),
                             createCodeExecutedQuestion());
   }
   
   private TabQuestion createPushTab() {
      String description = "push(Object) related question.";
      String methodProblemsTitle = "What is wrong?";
      CheckboxQuestion methodProblems = new CheckboxQuestion("methodProblems", 
                                                             methodProblemsTitle, 
                                                             description,
                                                             true, 
                                                             null, 
                                                             createNotUndefinedValueValidator(methodProblemsTitle), 
                                                             true,
                                                             new Choice("size is increased by 1", "SizeIncreased"), 
                                                             new Choice("size is not updated", "SizeNotUpdated"), 
                                                             new Choice("elements is replaced by a different array", "ElementsChanged"), 
                                                             new Choice("elements is not updated", "ElementsNotUpdated"), 
                                                             new Choice("Element at index size is replaced", "ElementAtSizeReplaced"), 
                                                             new Choice("Element at index size + 1 is replaced", "ElementAtSizePlusOneReplaced"), 
                                                             new Choice("Exception is thrown instead of updating the stack", "ExceptionThrown"), 
                                                             new Choice("Stack is updated instead of throwing an exception", "ExceptionNOtThrown"), 
                                                             new Choice("Executing pop after push would not return the added element.", "PushPopBroken"), 
                                                             createElseWrongChoice(description));
      String implementedAsDocumentedTitle = "Does the method implementation operates as specified by its JavaDoc comment?";
      RadioButtonsQuestion implementedAsDocumented = new RadioButtonsQuestion("implementedAsDocumented", 
                                                                              implementedAsDocumentedTitle, 
                                                                              description,
                                                                              true, 
                                                                              null, 
                                                                              createNotUndefinedValueValidator(implementedAsDocumentedTitle), 
                                                                              true,
                                                                              new Choice("Yes", "Yes", true), 
                                                                              new Choice("No", "No", methodProblems));
      String executedTitle = createExecutedQuestion("push(Object)");
      CheckboxQuestion executedQuestion = new CheckboxQuestion("executedStatements", 
                                                               executedTitle, 
                                                               description,
                                                               true,
                                                               null, 
                                                               new NotUndefinedValueValidator("Question '" + executedTitle + "' not answered."), 
                                                               true,
                                                               new Choice("None of the statements can be executed", "None"),
                                                               new Choice("Line 55: if (size < elements.length)", "Line 55", true),
                                                               new Choice("Line 56: elements[size++] = e", "Line 56", true),
                                                               new Choice("Line 59: throw new IllegalStateException(\"Stack is full.\")", "Line 59", true));
      return new TabQuestion("push(Object)", 
                             "push(Object)", 
                             false, 
                             implementedAsDocumented,
                             createStackClassInvariantQuestion(description, false, false),
                             createThrownExceptionsQuestion(description, false, false, true, false, false, false),
                             executedQuestion,
                             createStackLocationQuestion(description, false, true, true),
                             createSEDUsedQuestion(),
                             createCodeExecutedQuestion());
   }
   
   private TabQuestion createPopTab() {
      String description = "pop() related question.";
      String methodProblemsTitle = "What is wrong?";
      CheckboxQuestion methodProblems = new CheckboxQuestion("methodProblems", 
                                                             methodProblemsTitle, 
                                                             description,
                                                             true, 
                                                             null, 
                                                             createNotUndefinedValueValidator(methodProblemsTitle), 
                                                             true,
                                                             new Choice("size is decreased by 1", "SizeDecrease"), 
                                                             new Choice("size is not updated", "SizeNotUpdated"), 
                                                             new Choice("elements is replaced by a different array", "ElementsChanged"), 
                                                             new Choice("elements is not updated", "ElementsNotUpdated"), 
                                                             new Choice("Element at index size is returned", "ElementAtSizeReturned"), 
                                                             new Choice("Element at index size - 1 is returned", "ElementAtSizePlusOneReturned"), 
                                                             new Choice("Exception is thrown instead of returning the top element", "ExceptionThrown"), 
                                                             new Choice("Top element is returned instead of throwing an exception", "ExceptionNOtThrown"), 
                                                             new Choice("Executing pop twice would return the same element twice.", "PopPopBroken"), 
                                                             createElseWrongChoice(description));
      String implementedAsDocumentedTitle = "Does the method implementation operates as specified by its JavaDoc comment?";
      RadioButtonsQuestion implementedAsDocumented = new RadioButtonsQuestion("implementedAsDocumented", 
                                                                              implementedAsDocumentedTitle, 
                                                                              description,
                                                                              true, 
                                                                              null, 
                                                                              createNotUndefinedValueValidator(implementedAsDocumentedTitle), 
                                                                              true,
                                                                              new Choice("Yes", "Yes", true), 
                                                                              new Choice("No", "No", methodProblems));
      String returnValueTitle = "Which claims about the returned value are true?";
      CheckboxQuestion returnValue = new CheckboxQuestion("returnValue", 
                                                          returnValueTitle, 
                                                          description,
                                                          true,
                                                          null, 
                                                          new NotUndefinedValueValidator("Question '" + returnValueTitle + "' not answered."), 
                                                          true,
                                                          new Choice("null might be returned", "NullReturned", true),
                                                          new Choice("An object might be returned", "ObjectReturned", true),
                                                          new Choice("Element at index size is returned", "ElementAtSizeReturned"),
                                                          new Choice("Element at index size - 1 is returned", "ElementAtSizePlusOneReturned", true));
      String executedTitle = createExecutedQuestion("pop()");
      CheckboxQuestion executedQuestion = new CheckboxQuestion("executedStatements", 
                                                               executedTitle, 
                                                               description,
                                                               true,
                                                               null, 
                                                               new NotUndefinedValueValidator("Question '" + executedTitle + "' not answered."), 
                                                               true,
                                                               new Choice("None of the statements can be executed", "None"),
                                                               new Choice("Line 71: if (size >= 1)", "Line 71", true),
                                                               new Choice("Line 72: return elements[--size]", "Line 72", true),
                                                               new Choice("Line 75: throw new IllegalStateException(\"Stack is empty.\")", "Line 75", true));
      return new TabQuestion("pop()", 
                             "pop()", 
                             false, 
                             implementedAsDocumented,
                             createStackClassInvariantQuestion(description, false, true),
                             createThrownExceptionsQuestion(description, false, false, true, false, false, false),
                             executedQuestion,
                             createStackLocationQuestion(description, false, true, false),
                             returnValue,
                             createSEDUsedQuestion(),
                             createCodeExecutedQuestion());
   }
   
   private RadioButtonsQuestion createSEDUsedQuestion() {
      String title = "Does the symbolic execution tree help to answer the questions?";
      return new RadioButtonsQuestion("setConsidered", 
                                      title, 
                                      (String) null,
                                      true, 
                                      null, 
                                      createNotUndefinedValueValidator(title), 
                                      false,
                                      new Tool[] {getTool(SED_TOOL_NAME)},
                                      new Choice("Yes, Very helpful", "YesVeryHelpful"), 
                                      new Choice("Yes, Helpful", "YesHelpful"), 
                                      new Choice("Yes, Little helpful", "YesLittleHelpful"), 
                                      new Choice("No, Not helpful", "NoNotHelpful"),
                                      new Choice("Not considered", "NotConsidered"));
   }
   
   private RadioButtonsQuestion createCodeExecutedQuestion() {
      String helpfulTitle = "Does executing/debugging the source code help to answer the questions?";
      RadioButtonsQuestion helpfulQuestion = new RadioButtonsQuestion("executionHelpful", 
                                                                      helpfulTitle, 
                                                                      (String) null,
                                                                      true, 
                                                                      null, 
                                                                      createNotUndefinedValueValidator(helpfulTitle), 
                                                                      false,
                                                                      new Tool[] {getTool(NO_TOOL_NAME)},
                                                                      new Choice("Yes, Very helpful", "YesVeryHelpful"), 
                                                                      new Choice("Yes, Helpful", "YesHelpful"), 
                                                                      new Choice("Yes, Little helpful", "YesLittleHelpful"), 
                                                                      new Choice("No, Not helpful", "NoNotHelpful"));
      String writtenCodetitle = "Which code has been written?";
      TextQuestion writtenCodeQuestion = new TextQuestion("writtenCode", 
                                                          writtenCodetitle, 
                                                          "(Only if code is still available.)", 
                                                          null, 
                                                          null, 
                                                          false,
                                                          new Tool[] {getTool(NO_TOOL_NAME)});
      String title = "Have you executed/debugged the source code to answer the questions?";
      return new RadioButtonsQuestion("codeExecuted", 
                                      title, 
                                      (String) null,
                                      true, 
                                      null, 
                                      createNotUndefinedValueValidator(title), 
                                      false,
                                      new Tool[] {getTool(NO_TOOL_NAME)},
                                      new Choice("Yes", "Yes", helpfulQuestion, writtenCodeQuestion), 
                                      new Choice("No", "No"));
   }

   private Choice createElseRetrunedChoice(String description) {
      return new Choice("Something else is returned", "SomethingElse", createElseReturnedSubQuestion(description));
   }

   private TextQuestion createElseReturnedSubQuestion(String description) {
      String title = "What is returned?";
      return new TextQuestion("whatsReturned", title, description, null, new NotUndefinedValueValidator("Question '" + title + "' not answered."), false);
   }

   private Choice createElseWrongChoice(String description) {
      return new Choice("Something else is wrong", "SomethingElse", createElseWrongSubQuestion(description));
   }

   private TextQuestion createElseWrongSubQuestion(String description) {
      String title = "What is wrong?";
      return new TextQuestion("whatsWrong", title, description, null, new NotUndefinedValueValidator("Question '" + title + "' not answered."), false);
   }
   
   private RadioButtonsQuestion createStackClassInvariantQuestion(String description, boolean constructor, boolean expectedMemoryLeak) {
      String problemsTitle = "What is wrong?";
      CheckboxQuestion problems = new CheckboxQuestion("classInvariantProblems", 
                                                       problemsTitle, 
                                                       description,
                                                       true, 
                                                       null, 
                                                       createNotUndefinedValueValidator(problemsTitle), 
                                                       true,
                                                       new Choice("elements might be null.", "ElementsNull"), 
                                                       new Choice("elements might be non null.", "ElementsNonNull"), 
                                                       new Choice("elements might be of type Object[].", "ElementsTypeObjectArray"), 
                                                       new Choice("elements might be not of type Object[].", "ElementsTypeNotObjectArray"), 
                                                       new Choice("Element at index < size might be null", "ContainedElementNull"), 
                                                       new Choice("Element at index < size might be non null", "ContainedElementNonNull"), 
                                                       new Choice("Element at index >= size might be null", "NotContainedElementNull"), 
                                                       new Choice("Element at index >= size might be non null", "NotContainedElementNonNull", expectedMemoryLeak), 
                                                       new Choice("size might be < 0", "NegativeSize"), 
                                                       new Choice("size might be < elements.length", "SizeLessArrayLength"), 
                                                       new Choice("size might be = elements.length", "SizeEqualArrayLength"), 
                                                       new Choice("size might be > elements.length", "SizeGreaterArrayLength"), 
                                                       createElseWrongChoice(description));
      String title = constructor ?
                     "Is the class invariant established?" :
                     "Is the class invariant preserved?";
      return new RadioButtonsQuestion("classInvariant", 
                                      title, 
                                      description,
                                      true, 
                                      null, 
                                      createNotUndefinedValueValidator(title), 
                                      true,
                                      new Choice("Yes", "Yes", !expectedMemoryLeak), 
                                      new Choice("No", "No", expectedMemoryLeak, problems));
   }

   private CheckboxQuestion createStackLocationQuestion(String description, boolean expectedElements, boolean expectedSize, boolean expectedElementAtPlus1) {
      String title = "Which location(s) of the initial state before method invocation might be changed during execution?";
      return new CheckboxQuestion("changedLocations", 
                                  title, 
                                  description,
                                  true,
                                  null, 
                                  new NotUndefinedValueValidator("Question '" + title + "' not answered."), 
                                  true,
                                  new Choice("None", "None"),
                                  new Choice("elements", "elements", expectedElements),
                                  new Choice("elements[size - 1]", "elements[size - 1]"),
                                  new Choice("elements[size]", "elements[size]"),
                                  new Choice("elements[size + 1]", "elements[size + 1]", expectedElementAtPlus1),
                                  new Choice("elements[*]", "elements[*]"),
                                  new Choice("elements.length", "elements.length"),
                                  new Choice("size", "size", expectedSize),
                                  new Choice("something else", "SomethingElse", createElseLocationSubQuestion(description)));
   }
   
   private TextQuestion createElseLocationSubQuestion(String description) {
      String locationTitle = "Which additional location(s) can be changed?";
      return new TextQuestion("elseLocation", locationTitle, description, null, new NotUndefinedValueValidator("Question '" + locationTitle + "' not answered."), false);
   }
   
   private RadioButtonsQuestion createThrownExceptionsQuestion(String description, boolean expectedNPE, boolean expectedNASE, boolean expectedISE, boolean expectedIAE, boolean expectedAIOOBE, boolean expectedASE) {
      String title = "Is it possible that an exception is thrown?";
      return new RadioButtonsQuestion("exceptionThrown", 
                                      title, 
                                      description,
                                      true, 
                                      null, 
                                      createNotUndefinedValueValidator(title), 
                                      true,
                                      new Choice("Yes", "Yes", !expectedNPE && !expectedNASE && !expectedISE, createThrownExceptionsSubQuestion(description, expectedNPE, expectedNASE, expectedISE, expectedIAE, expectedAIOOBE, expectedASE)), 
                                      new Choice("No", "No"));
   }
   
   private CheckboxQuestion createThrownExceptionsSubQuestion(String description, boolean expectedNPE, boolean expectedNASE, boolean expectedISE, boolean expectedIAE, boolean expectedAIOOBE, boolean expectedASE) {
      String thrownExceptionTitle = "Which exception(s) might be thrown?";
      CheckboxQuestion thrownExceptionQuestion = new CheckboxQuestion("whichExceptionsMightBeThrown", 
                                                                      thrownExceptionTitle, 
                                                                      description,
                                                                      true,
                                                                      null, 
                                                                      new NotUndefinedValueValidator("Question '" + thrownExceptionTitle + "' not answered."), 
                                                                      true,
                                                                      new Choice("java.lang.NullPointerException", "java.lang.NullPointerException", expectedNPE),
                                                                      new Choice("java.lang.ArithmeticException", "java.lang.ArithmeticException"),
                                                                      new Choice("java.lang.ArrayIndexOutOfBoundsException", "java.lang.ArrayIndexOutOfBoundsException", expectedAIOOBE),
                                                                      new Choice("java.lang.ArrayStoreException", "java.lang.ArrayStoreException", expectedASE),
                                                                      new Choice("java.lang.NegativeArraySizeException", "java.lang.NegativeArraySizeException", expectedNASE),
                                                                      new Choice("java.lang.IllegalArgumentException", "java.lang.IllegalArgumentException", expectedIAE),
                                                                      new Choice("java.lang.IllegalStateException", "java.lang.IllegalStateException", expectedISE),
                                                                      new Choice("something else", "SomethingElse", createElseExceptionSubQuestion(description)));
      return thrownExceptionQuestion;
   }

   private TextQuestion createElseExceptionSubQuestion(String description) {
      String exceptionTitle = "Which exception is thrown?";
      return new TextQuestion("thrownException", exceptionTitle, description, null, new NotUndefinedValueValidator("Question '" + exceptionTitle + "' not answered."), false);
   }
   
   private IValueValidator createNotUndefinedValueValidator(String questionTitle) {
      return new NotUndefinedValueValidator("Question '" + questionTitle + "' not answered.");
   }

   private String createGeneralClassDescription(String className) {
      return "Please inspect the current source code of class '" + className + "' carefully and answer the following questions about it as best as possible.";
   }

   protected String createQuestionPageMessage() {
      return "Please answer the questions to the best of your knowledge.";
   }
   
   protected String createExecutedQuestion(String startMethod) {
      return "Which statement(s) can be executed starting at " + startMethod + "?";
   }
   
   public RandomForm getEvaluationForm() {
      return (RandomForm) getForm(EVALUATION_FORM_NAME);
   }
}