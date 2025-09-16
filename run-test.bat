@echo off
cd /d "d:\Documents\GitHub\Diadem"
java -cp "target\classes;%USERPROFILE%\.m2\repository\org\graalvm\polyglot\polyglot\24.2.2\polyglot-24.2.2.jar;%USERPROFILE%\.m2\repository\org\graalvm\polyglot\js-community\24.2.2\js-community-24.2.2.jar;%USERPROFILE%\.m2\repository\org\lwjgl\lwjgl\3.3.3\lwjgl-3.3.3.jar;%USERPROFILE%\.m2\repository\org\lwjgl\lwjgl-glfw\3.3.3\lwjgl-glfw-3.3.3.jar;%USERPROFILE%\.m2\repository\org\lwjgl\lwjgl-opengl\3.3.3\lwjgl-opengl-3.3.3.jar;%USERPROFILE%\.m2\repository\org\lwjgl\lwjgl\3.3.3\lwjgl-3.3.3-natives-windows.jar;%USERPROFILE%\.m2\repository\org\lwjgl\lwjgl-glfw\3.3.3\lwjgl-glfw-3.3.3-natives-windows.jar;%USERPROFILE%\.m2\repository\org\lwjgl\lwjgl-opengl\3.3.3\lwjgl-opengl-3.3.3-natives-windows.jar" black.alias.diadem.ThreeJSTest
pause
