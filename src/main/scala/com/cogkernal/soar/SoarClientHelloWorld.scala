package com.cogkernal.soar

import sml.{Agent, Kernel, smlPrintEventId}

import java.io.File

/** Hello-World demonstrating the Soar SML Java client from Scala.
 *
 *  Run with:
 *    sbt "runMain com.cogkernal.soar.SoarClientHelloWorld"
 *
 *  build.sbt sets -Djava.library.path to the Soar out/ directory so the JVM
 *  can find libSoar.dylib and libJava_sml_ClientInterface.jnilib.
 */
object SoarClientHelloWorld:

  def main(args: Array[String]): Unit =

    System.load(new File("../Soar/out/libJava_sml_ClientInterface.jnilib").getAbsolutePath)

    println(System.getProperty("java.library.path"))
    println("=== Soar SML Hello World (Scala) ===")

    // 1. Create an in-process kernel (no socket, runs in this thread)
    val kernel = Kernel.CreateKernelInCurrentThread()
    require(kernel != null, "Failed to create Soar kernel")
    require(!kernel.HadError(), s"Kernel error: ${kernel.GetLastErrorDescription()}")
    println(s"Kernel created — Soar ${kernel.GetSoarKernelVersion()}")

    // 2. Create a named agent
    val agent = kernel.CreateAgent("hello-agent")
    require(agent != null && !kernel.HadError(),
      s"Agent creation failed: ${kernel.GetLastErrorDescription()}")
    println(s"Agent created  — '${agent.GetAgentName()}'")

    // 3. Capture print output from the Soar trace
    val printListener = new Agent.PrintEventInterface:
      def printEventHandler(eventID: Int, data: Object, agent: Agent, message: String): Unit =
        print(message)

    val printCB = agent.RegisterForPrintEvent(
      smlPrintEventId.smlEVENT_PRINT, printListener, null, /*ignoreOwn=*/true)

    // 4. Load a production inline via the command interface.
    //    The rule fires once on the first decision cycle, writes to the trace,
    //    then halts the agent.
    val production =
      """sp {hello-world
        |  (state <s> ^superstate nil)
        |-->
        |  (write |Hello, World! Soar is running.| (crlf))
        |  (halt)
        |}""".stripMargin

    val spResult = agent.ExecuteCommandLine(production)
    println(s"Production loaded: ${spResult.trim}")

    // 5. Run the agent — the halt RHS action stops it after the first DC
    println("\n--- Running agent ---")
    agent.RunSelf(50)
    println("--- Agent halted ---\n")

    // 6. Query simple stats via the CLI
    val stats = agent.ExecuteCommandLine("stats")
    println(s"Stats:\n$stats")

    // 7. Tear down
    agent.UnregisterForPrintEvent(printCB)
    kernel.DestroyAgent(agent)
    kernel.Shutdown()
    println("Shutdown complete.")
