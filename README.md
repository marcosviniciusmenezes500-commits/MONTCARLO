# DroidScope

Aplicativo Android nativo (Kotlin + Jetpack Compose + Material 3) de monitoramento de hardware, telemetria de bateria, benchmarking e diagnóstico do sistema.

## Funcionalidades Implementadas
- Arquitetura: Clean/MVVM, Hilt + KSP, Room (:core-database), Preferences DataStore, Coroutines/Flow (Dispatchers.IO) e WorkManager.
- Navegação: 9 seções completas (Dashboard, Bateria, Benchmark, Hardware, Monitoramento, Sensores, Diagnóstico, Histórico e Configurações).
- Bateria (AccuBattery): Percentual, status, saúde, tecnologia, temperatura, tensão, corrente instantânea (µA), ChargeCounter (µAh), potência estimada (W), ciclos (Android 14+), gráficos ao vivo e estimativa transparente de capacidade total.
- Benchmark Nativo (DroidScope Score): CPU Single-Core e Multi-Core (primos/inteiros, ponto flutuante, SHA-256, compressão Deflater), banda de RAM, I/O de armazenamento (sequencial + aleatório 4 KB em cacheDir) e GPU offscreen (OpenGL ES via EGL14), com temperatura inicial/final e histórico no Room.
- Hardware e Rede (AIDA64/DevCheck): SoC, ABIs, frequências por núcleo, RAM, armazenamento, tela (resolução, DPI, Hz, HDR, brilho), câmeras via Camera2 (MP, sensor físico, distância focal, abertura, OIS), IP local, link estimado, Bluetooth, NFC e TrafficStats.
- Sensores: Listagem completa + leitura ao vivo (SensorEventListener) sob demanda ao tocar em qualquer sensor.
- Diagnóstico e Exportação: Motor de regras reais (temperatura, thermal throttling via PowerManager, RAM, armazenamento, saúde da bateria) salvo no Room, exportação via Share Sheet e limpeza configurável.

## Limitações Conhecidas (Android sem Root)
- Frequências de CPU (/sys/devices/system/cpu): Alguns fabricantes/kernels restringem leitura via SELinux; o app exibe N/D sem simular valores.
- ChargeCounter e Ciclos: EXTRA_CYCLE_COUNT requer Android 14+ (API 34) e exposição pelo fabricante.
- GPU Benchmark: Requer suporte a EGL14 Pbuffer OpenGL ES 2.0+.
- WorkManager: Intervalo mínimo de coleta em segundo plano é limitado pelo Android a 15 minutos.

## Build
    ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease --no-daemon
