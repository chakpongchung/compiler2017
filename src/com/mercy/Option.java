package com.mercy;

/**
 * Created by mercy on 17-5-20.
 */
public class Option {
    // CONSTANT
    public static int REG_SIZE = 8;
    public static int STACK_VAR_ALIGNMENT_SIZE = 4;
    public static int FRAME_ALIGNMENT_SIZE = 16;

    // I/O
    public static String inFile = "testcase/test.c";
    public static String outFile = "out.asm";

    // DEBUG
    public static boolean printInsturction = false;
    public static boolean printBasicBlocks = true;
    public static boolean printRemoveInfo = false;
    public static boolean printInlineInfo = true;

    public static boolean printNaiveAllocatorInfo = true;

    // OPTIMIZATION
    public static boolean enableRegisterAllocation = true;

    public static boolean enableInstructionSelection        = true;
    public static boolean enableInlineFunction              = true;
    public static boolean enableCommonExpressionElimination = true;

    public static boolean enableControlFlowOptimization     = true;
    public static boolean enableOutputIrrelevantElimination = false;

    // STEADY
    public static boolean enablePrintExpand = true;
}