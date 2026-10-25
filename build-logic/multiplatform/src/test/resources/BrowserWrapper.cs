using System;
using System.ComponentModel;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;

public static class BrowserWrapper
{
    // Keep this handle open until process exit: killing the wrapper must also kill Chrome and its children.
    private static IntPtr job;

    public static int Main(string[] arguments)
    {
        bool explicitBrowser = Path.GetFileNameWithoutExtension(Environment.GetCommandLineArgs()[0]) == "explicit-chrome";
        if (arguments.Length == 1 && arguments[0] == "--version")
        {
            Console.WriteLine(Environment.GetEnvironmentVariable(explicitBrowser ? "TEST_EXPLICIT_CHROME_VERSION" : "TEST_DEFAULT_CHROME_VERSION"));
            return 0;
        }

        File.AppendAllText(Environment.GetEnvironmentVariable("TEST_CHROME_LAUNCH_LOG"), explicitBrowser ? "explicit\n" : "default\n");
        CreateChildProcessJob();
        StringBuilder commandLine = new StringBuilder();
        foreach (string argument in arguments)
        {
            if (commandLine.Length > 0) commandLine.Append(' ');
            commandLine.Append(QuoteArgument(argument));
        }
        ProcessStartInfo start = new ProcessStartInfo(Environment.GetEnvironmentVariable("TEST_CHROME_EXECUTABLE"), commandLine.ToString());
        start.UseShellExecute = false;
        using (Process browser = Process.Start(start))
        {
            browser.WaitForExit();
            return browser.ExitCode;
        }
    }

    private static string QuoteArgument(string argument)
    {
        StringBuilder quoted = new StringBuilder("\"");
        int backslashes = 0;
        foreach (char character in argument)
        {
            if (character == '\\') { backslashes++; continue; }
            quoted.Append('\\', character == '"' ? backslashes * 2 + 1 : backslashes);
            quoted.Append(character);
            backslashes = 0;
        }
        quoted.Append('\\', backslashes * 2);
        return quoted.Append('"').ToString();
    }

    private static void CreateChildProcessJob()
    {
        job = CreateJobObject(IntPtr.Zero, null);
        if (job == IntPtr.Zero) throw new Win32Exception();
        ExtendedLimits limits = new ExtendedLimits();
        limits.Basic.LimitFlags = 0x2000; // JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
        int size = Marshal.SizeOf(typeof(ExtendedLimits));
        IntPtr information = Marshal.AllocHGlobal(size);
        try
        {
            Marshal.StructureToPtr(limits, information, false);
            if (!SetInformationJobObject(job, 9, information, (uint)size)) throw new Win32Exception();
        }
        finally { Marshal.FreeHGlobal(information); }
        // Assign the wrapper first, so children inherit the job without a race after Process.Start.
        if (!AssignProcessToJobObject(job, Process.GetCurrentProcess().Handle)) throw new Win32Exception();
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct BasicLimits
    {
        public long PerProcessTime, PerJobTime;
        public uint LimitFlags;
        public UIntPtr MinimumWorkingSet, MaximumWorkingSet;
        public uint ActiveProcessLimit;
        public UIntPtr Affinity;
        public uint PriorityClass, SchedulingClass;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct IoCounters
    {
        public ulong ReadOperations, WriteOperations, OtherOperations, ReadBytes, WriteBytes, OtherBytes;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct ExtendedLimits
    {
        public BasicLimits Basic;
        public IoCounters Io;
        public UIntPtr ProcessMemory, JobMemory, PeakProcessMemory, PeakJobMemory;
    }

    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern IntPtr CreateJobObject(IntPtr attributes, string name);
    [DllImport("kernel32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool SetInformationJobObject(IntPtr job, int informationClass, IntPtr information, uint size);
    [DllImport("kernel32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool AssignProcessToJobObject(IntPtr job, IntPtr process);
}
