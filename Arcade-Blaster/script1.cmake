execute_process(COMMAND "${CMAKE_CURRENT_BINARY_DIR}/papilio_prog/papilio-prog" OUTPUT_VARIABLE out ERROR_VARIABLE outerr OUTPUT_STRIP_TRAILING_WHITESPACE)
message("stdout: ${out}")
if(out MATCHES "^No or ambiguous options specified")
message("ok")
else()
message(SEND_ERROR "fail")
endif()

