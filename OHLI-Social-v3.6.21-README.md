# OHLI Social v3.6.21

- Android versionName `3.6.21`; versionCode `50`.
- Sampul Sorotan dikunci ke lingkaran 1:1 pada strip profil dan grid Sorotan.
- Riwayat aktivitas berbentuk kartu media, dikelompokkan per tanggal, dengan pembukaan kembali profil/postingan jika masih diizinkan.
- APK mengirim notifikasi Android untuk aktivitas server selain OTP; seluruh pemberitahuan juga tersimpan di Pusat Notifikasi.
- Tidak memerlukan WhatsApp API. WhatsApp tetap manual; tidak ada pesan WA otomatis.

## Build
Unggah source ZIP ini ke root repository dan gunakan `.github/workflows/build-apk.yml` yang disertakan. Workflow mendeteksi root Gradle dan menjalankan `gradle :app:assembleDebug --stacktrace`. Artifact APK bernama `OHLI-Social-Server-APK-v3.6.21`.

## Pemeriksaan
Jalankan `node tools/source-verify.mjs`. Pemeriksaan statis tidak menggantikan build Gradle atau pengujian perangkat.
