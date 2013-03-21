// Copyright (c) 2013 , Yang Bo All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.dongxiguo.protobuf;

import com.dongxiguo.utils.HaxelibRun;
import sys.io.File;
import sys.FileSystem;

class Run
{
  static function main()
  {
    var args = Sys.args();
    var returnValue = HaxelibRun.run(args);
    if (returnValue == null)
    {
      switch (args[0])
      {
        case "protoc":
          var libPath = Sys.getCwd();
          var cwd = args.pop();
          Sys.setCwd(cwd);
          var pluginScriptFileName = cwd + (Sys.systemName() == "Windows" ?  "protoc-gen-as3.tmp.bat" : "protoc-gen-as3.tmp.sh");
	  File.saveContent(pluginScriptFileName, Sys.systemName() == "Windows" ? '@"$libPath\\protoc-gen-as3.bat"' : '#!/bin/sh
"$libPath/protoc-gen-as3"');
          if (Sys.systemName() == "Windows")
          {
            Sys.command("cacls", [pluginScriptFileName, "/E", "/G", "Everyone:R"]);
          }
          else
          {
            Sys.command("chmod", ["+x", pluginScriptFileName]);
          }
          args[0] = '--plugin=protoc-gen-as3=$pluginScriptFileName';
          var returnValue = Sys.command("protoc", args);
	  FileSystem.deleteFile(pluginScriptFileName);
          Sys.exit(returnValue);
        default:
          Sys.print('Usage: haxelib run ${HaxelibRun.libraryName()} protoc [ args ... ]
${HaxelibRun.usage()}');
          Sys.exit(-1);
      }
    }
    else
    {
      Sys.exit(returnValue);
    }
  }
}

// vim: sts=2 sw=2 et
