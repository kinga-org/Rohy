# BouncyCastle lightweight API is referenced reflectively by nothing; keep defaults.
-dontwarn org.bouncycastle.**

# ML Kit (QR code scanner) creates its components by reflection through Firebase Components. The library's own
# rule keeps the registrar classes but, in R8 full mode, not their no-argument constructor: the scanner then
# crashes on start with a NullPointerException (Rohy 0.1.0). Checked by scripts/check-r8-registrars.sh (CI, pre-push).
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
