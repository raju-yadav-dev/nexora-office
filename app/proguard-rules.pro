-dontwarn aQute.bnd.annotation.spi.**
-dontwarn com.github.javaparser.**
-dontwarn com.sun.org.apache.xml.internal.resolver.**
-dontwarn de.rototor.pdfbox.graphics2d.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
-dontwarn javax.xml.crypto.**
-dontwarn javax.xml.stream.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.batik.**
-dontwarn org.apache.jcp.xml.dsig.internal.dom.**
-dontwarn org.apache.maven.**
-dontwarn org.apache.pdfbox.**
-dontwarn org.apache.tools.ant.**
-dontwarn org.apache.xml.security.**
-dontwarn org.bouncycastle.**
-dontwarn org.ietf.jgss.**
-dontwarn org.osgi.framework.**
-dontwarn org.w3c.dom.events.**
-dontwarn org.w3c.dom.svg.**
-dontwarn org.w3c.dom.traversal.**

# Apache POI and XMLBeans include optional desktop/server integrations that are
# not available on Android. Keep the document model reachable while allowing R8
# to ignore those optional integration points.
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
