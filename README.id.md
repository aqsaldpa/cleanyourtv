# CleanYourTV, versi ringkas Bahasa Indonesia

Bikin Android TV / Google TV lebih ringan lewat Wi-Fi: matikan app bawaan yang tidak dipakai, lalu ganti
home screen dengan launcher ringan (±58 MB, Google TV ±130 MB). Tanpa root, tanpa kabel, tidak ada yang
di-uninstall, dan semuanya bisa dikembalikan.

## Yang perlu disiapkan

- Komputer dan TV di Wi-Fi yang sama (bukan Wi-Fi tamu)
- Python 3, JDK 17, Android SDK platform 34 (untuk build launcher), `adb`
- Di TV: Setelan > Preferensi Perangkat > Tentang > tekan **Build** 7x > kembali > Opsi developer >
  **USB debugging** nyala

## Langkah

```bash
git clone https://github.com/aqsaldpa/cleanyourtv && cd cleanyourtv
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt

tools/find-tv.sh                       # cari TV otomatis, tidak perlu tahu IP
export TV_HOST=192.168.1.3             # IP dari hasil di atas

tools/disable.sh examples/polytron-4k-lushan/debloat.txt   # atau daftar model TV kamu di examples/
launcher/build-and-install.sh          # pasang launcher ringan
tools/lite.sh                          # mode ringan: animasi 0.5x, batasi app background, laporan RAM
```

Muncul "Allow USB debugging?" di TV: centang **Always allow**, lalu OK.

## Kalau Home kembali ke Google TV

Wi-Fi yang disimpan saat setup awal dimiliki `setupwraith`. Di TV: Setelan > Jaringan > pilih Wi-Fi >
**Lupakan**, lalu sambungkan lagi. Cek juga nama Wi-Fi lama yang mirip (beda huruf besar/kecil): hapus
yang tidak dipakai, lalu jalankan `tools/disable.sh` dan `launcher/build-and-install.sh` lagi.

## Mengembalikan

```bash
tools/lite.sh undo
tools/restore.sh examples/polytron-4k-lushan/debloat.txt
```

## Hasil di Polytron 4K (lushan, 1.8 GB RAM)

| | Sebelum | Sesudah |
|---|---|---|
| RAM tersedia | 370 MB | 750 MB |
| Swap kosong | 219 MB | 473 MB |
| Launcher | Google TV ±130 MB | 58 MB |
