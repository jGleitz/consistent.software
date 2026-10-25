$ErrorActionPreference = 'Stop'
# Windows PowerShell's built-in .NET compiler produces an executable that Node can spawn without a shell.
Add-Type -Path $env:TEST_WRAPPER_SOURCE -OutputAssembly $env:TEST_WRAPPER_OUTPUT -OutputType ConsoleApplication
