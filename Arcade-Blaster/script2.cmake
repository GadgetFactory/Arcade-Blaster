execute_process(COMMAND "${CMAKE_CURRENT_BINARY_DIR}/../programmer/linux32/data2mem" OUTPUT_VARIABLE outme ERROR_VARIABLE outerr OUTPUT_STRIP_TRAILING_WHITESPACE ERROR_STRIP_TRAILING_WHITESPACE)
message("Test ${outerr}")
message("Test ${outme}")
if(outme MATCHES "^\nRelease 12.2 - Data2MEM")
message("ok")
else()
message(SEND_ERROR "fail")
endif()

