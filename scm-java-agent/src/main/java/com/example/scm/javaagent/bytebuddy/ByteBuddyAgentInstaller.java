package com.example.scm.javaagent.bytebuddy;

import com.example.scm.javaagent.asm.AsmClassPrinter;
import com.example.scm.javaagent.config.AgentConfig;
import com.example.scm.javaagent.config.AgentRuntimeConfig;
import com.example.scm.javaagent.logging.AgentLogger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isNative;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * Byte Buddy 安装器，负责对 AI Agent 核心链路做方法耗时插桩。
 */
public class ByteBuddyAgentInstaller {

    private final AgentConfig config;
    private final AsmClassPrinter asmClassPrinter;

    public ByteBuddyAgentInstaller(AgentConfig config, AsmClassPrinter asmClassPrinter) {
        this.config = config;
        this.asmClassPrinter = asmClassPrinter;
    }

    public void install(Instrumentation instrumentation) {
        AgentRuntimeConfig.set(config);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.Adapter() {
            @Override
            public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                                         boolean loaded, DynamicType dynamicType) {
                AgentLogger.info("Byte Buddy transformed class=" + typeDescription.getName());
            }

            @Override
            public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                AgentLogger.error("Byte Buddy transform failed, class=" + typeName, throwable);
            }
        };

        new AgentBuilder.Default()
                .ignore(nameStartsWith("net.bytebuddy.")
                        .or(nameStartsWith("org.objectweb.asm."))
                        .or(nameStartsWith("com.example.scm.javaagent."))
                        .or(nameStartsWith("java."))
                        .or(nameStartsWith("jdk."))
                        .or(nameStartsWith("sun.")))
                .type(typeDescription -> {
                    try {
                        return config.shouldTraceKnownAiAgentClass(typeDescription.getName());
                    } catch (Throwable ex) {
                        AgentLogger.warn("Byte Buddy matcher skipped, errorType=" + ex.getClass().getName());
                        return false;
                    }
                })
                .transform(this::transform)
                .with(listener)
                .installOn(instrumentation);

        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                try {
                    if (className != null && config.asmPrint()) {
                        String dotted = className.replace('/', '.');
                        if (config.shouldTraceKnownAiAgentClass(dotted)) {
                            asmClassPrinter.print(classfileBuffer);
                        }
                    }
                } catch (Throwable ex) {
                    AgentLogger.warn("ASM transformer skipped, errorType=" + ex.getClass().getName());
                }
                return null;
            }
        }, true);
        AgentLogger.info("Byte Buddy instrumentation installed");
    }

    private DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                             TypeDescription typeDescription,
                                             ClassLoader classLoader,
                                             JavaModule module,
                                             ProtectionDomain protectionDomain) {
        try {
            return builder.visit(Advice.to(MethodTimingAdvice.class)
                    .on(not(isConstructor())
                            .and(not(isStatic()))
                            .and(not(isAbstract()))
                            .and(not(isNative()))
                            .and(ElementMatchers.not(ElementMatchers.nameStartsWith("lambda$")))));
        } catch (Throwable ex) {
            AgentLogger.warn("Byte Buddy transform skipped, class=" + typeDescription.getName()
                    + ", errorType=" + ex.getClass().getName());
            return builder;
        }
    }
}
