package com.example.scm.javaagent.asm;

import com.example.scm.javaagent.config.AgentConfig;
import com.example.scm.javaagent.logging.AgentLogger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM 只读解析示例，用于展示 class/method 元信息读取能力。
 */
public class AsmClassPrinter {

    private final AgentConfig config;

    public AsmClassPrinter(AgentConfig config) {
        this.config = config;
    }

    public void print(byte[] classfileBuffer) {
        if (!config.asmPrint() || classfileBuffer == null || classfileBuffer.length == 0) {
            return;
        }
        try {
            ClassReader reader = new ClassReader(classfileBuffer);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                private String className;

                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    className = name == null ? "" : name.replace('/', '.');
                    if (config.shouldTraceKnownAiAgentClass(className)) {
                        AgentLogger.info("[ASM] class=" + className + ", super=" + (superName == null ? "" : superName.replace('/', '.')));
                    }
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if (className != null && config.shouldTraceKnownAiAgentClass(className)
                            && !"<init>".equals(name) && !"<clinit>".equals(name)) {
                        AgentLogger.info("[ASM] method=" + className + "#" + name + ", desc=" + descriptor);
                    }
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (Throwable ex) {
            AgentLogger.warn("[ASM] class parse skipped, errorType=" + ex.getClass().getName());
        }
    }
}
