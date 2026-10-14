# Navigation 3 -avaimet sarjallistetaan kotlinx.serializationilla (paluupino tallentuu prosessin kuollessa).
-keepclassmembers @kotlinx.serialization.Serializable class fi.sateenkaarikello.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class fi.sateenkaarikello.**$$serializer { *; }
