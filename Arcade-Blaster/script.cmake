execute_process(COMMAND "${CMAKE_CURRENT_BINARY_DIR}/romgen/romgen" OUTPUT_VARIABLE out ERROR_VARIABLE outerr OUTPUT_STRIP_TRAILING_WHITESPACE)
message("stderr: ${outerr}")
if(outerr MATCHES "^romgen by MikeJ version 3.00")
message("ok")
else()
message(SEND_ERROR "fail")
endif()


