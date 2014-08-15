cmake_minimum_required (VERSION 2.8)

find_package(Java REQUIRED)
include(UseJava)
enable_testing()
project (gadget-shared)

list(APPEND gadget-shared_sources 
src/net/gadgetfactory/shared/AppSettings.java 
src/net/gadgetfactory/shared/HelperFunctions.java 
src/net/gadgetfactory/shared/MessageConsumer.java 
src/net/gadgetfactory/shared/MessageSiphon.java 
src/net/gadgetfactory/shared/UIHelpers.java
)

add_jar(gadget-shared ${gadget-shared_sources})

project (PapilioArcade)

list(APPEND java_sources 
src/net/gadgetfactory/papilio/arcade/ArcadeTheme.java 
src/net/gadgetfactory/papilio/arcade/AsyncWebImageBox.java
src/net/gadgetfactory/papilio/arcade/AWTUtilitiesWrapper.java
src/net/gadgetfactory/papilio/arcade/GamePadPanel.java
src/net/gadgetfactory/papilio/arcade/GamesListbox.java
src/net/gadgetfactory/papilio/arcade/HowtoPlayDialog.java
src/net/gadgetfactory/papilio/arcade/ImageButton.java
src/net/gadgetfactory/papilio/arcade/PapilioArcade.java
src/net/gadgetfactory/papilio/arcade/PreferencesDialog.java
src/net/gadgetfactory/papilio/arcade/UpdateDialog.java
src/net/gadgetfactory/papilio/arcade/WrJoysticksArray.java
src/net/gadgetfactory/papilio/arcade/ProgressGlassPane.java
)
set(CMAKE_JAVA_INCLUDE_PATH ${CMAKE_CURRENT_BINARY_DIR}/gadget-shared.jar)
add_jar(papilio_arcade ${java_sources} )

project (Tools)
find_package(PkgConfig)
pkg_check_modules(GLIB REQUIRED glib-2.0>=2.0.0)
pkg_check_modules(libftdi REQUIRED libftdi>=0.19)
include(CheckIncludeFiles)
add_subdirectory(papilio_prog)
add_subdirectory(romgen)
INSTALL(DIRECTORY roms hardware help images programmer DESTINATION PapilioArcade  USE_SOURCE_PERMISSIONS)
INSTALL(FILES start.sh PERMISSIONS OWNER_EXECUTE OWNER_READ DESTINATION PapilioArcade)
INSTALL(FILES ${CMAKE_CURRENT_BINARY_DIR}/papilio_arcade.jar PERMISSIONS OWNER_READ DESTINATION PapilioArcade)
INSTALL(FILES ${CMAKE_CURRENT_BINARY_DIR}/gadget-shared.jar PERMISSIONS OWNER_READ DESTINATION PapilioArcade)

add_test(NAME romgen COMMAND ${CMAKE_COMMAND} -P ${CMAKE_CURRENT_SOURCE_DIR}/script.cmake)
add_test(NAME papilio-prog COMMAND ${CMAKE_COMMAND} -P ${CMAKE_CURRENT_SOURCE_DIR}/script1.cmake)
add_test(NAME data2mem COMMAND ${CMAKE_COMMAND} -P ${CMAKE_CURRENT_SOURCE_DIR}/script2.cmake)


