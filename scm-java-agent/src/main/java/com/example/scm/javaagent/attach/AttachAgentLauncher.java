package com.example.scm.javaagent.attach;

import com.sun.tools.attach.VirtualMachine;

/**
 * 运行时热挂载工具，用于 agentmain 演示。
 */
public final class AttachAgentLauncher {

    private AttachAgentLauncher() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java --add-modules jdk.attach -cp scm-java-agent.jar "
                    + "com.example.scm.javaagent.attach.AttachAgentLauncher <pid> <agentJarPath> [agentArgs]");
            return;
        }
        String pid = args[0];
        String agentJarPath = args[1];
        String agentArgs = args.length >= 3 ? args[2] : "";
        VirtualMachine virtualMachine = VirtualMachine.attach(pid);
        try {
            virtualMachine.loadAgent(agentJarPath, agentArgs);
            System.out.println("SCM Java Agent attached, pid=" + pid + ", agentJar=" + agentJarPath);
        } finally {
            virtualMachine.detach();
        }
    }
}
