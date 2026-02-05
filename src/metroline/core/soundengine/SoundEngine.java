package metroline.core.soundengine;


import metroline.core.soundengine.decoder.exceptions.JavaLayerException;
import metroline.util.MetroLogger;
import metroline.util.serialize.GlobalSettings;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SoundEngine {

    private float globalVolume = 1.0f;
    private float musicVolume = 1.0f;
    private float sfxVolume = 1.0f;
    private final MusicPlayer musicPlayer;

    private static SoundEngine instance;
    private final AudioMixer audioMixer;
    private final Map<String, Sound> soundCache;
    private final Map<String, Clip> uiClipCache;
    private final AudioFormat defaultFormat;

    private SoundEngine() throws LineUnavailableException {
        // Создаем стандартный формат
        this.defaultFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100,
                16,
                2, // Стерео выход
                 4,
                44100,
                false
        );
        this.audioMixer = new AudioMixer(defaultFormat);
        this.musicPlayer = new MusicPlayer(audioMixer);
        this.soundCache = new HashMap<>();
        this.uiClipCache = new HashMap<>();
        this.audioMixer.start();
    }
    public void preloadUIClip(String key) {
        if (uiClipCache.containsKey(key)) return;

        Sound sound = soundCache.get(key);
        if (sound == null) {
            MetroLogger.logWarning("Cannot preload UI Clip: sound not found - " + key);
            return;
        }

        try {
            Clip clip = AudioSystem.getClip();
            byte[] audioData = sound.getPcmDataAsBytes();
            AudioFormat format = sound.getFormat();

            // Clip требует формат с frameSize и frameRate
            AudioFormat clipFormat = new AudioFormat(
                    format.getEncoding(),
                    format.getSampleRate(),
                    format.getSampleSizeInBits(),
                    format.getChannels(),
                    format.getFrameSize(),
                    format.getFrameRate(),
                    format.isBigEndian()
            );

            AudioInputStream ais = new AudioInputStream(
                    new java.io.ByteArrayInputStream(audioData),
                    clipFormat,
                    audioData.length / format.getFrameSize()
            );

            clip.open(ais);
            uiClipCache.put(key, clip);
            MetroLogger.logInfo("UI Clip preloaded: " + key);

        } catch (Exception e) {
            MetroLogger.logError("Failed to preload UI Clip: " + key, e);
        }
    }
    public void playMusic(String key, float volume, boolean loop) {
        Sound sound = soundCache.get(key);
        if (sound == null) {
            throw new IllegalArgumentException("Music with key '" + key + "' not found");
        }

        if (!isFormatCompatible(sound.getFormat(), defaultFormat)) {
            MetroLogger.logInfo("Warning: Music format " + sound.getFormat() +
                    " doesn't match output format " + defaultFormat);
        }

        musicPlayer.play(key, sound, volume, loop);
    }
    /**
     * Плавно переходит к указанному музыкальному треку.
     * @param key ключ трека
     * @param targetVolume целевая громкость (до применения musicVolume и globalVolume)
     * @param fadeTimeSeconds длительность фейда в секундах
     */
    public void fadeToMusic(String key, float targetVolume, float fadeTimeSeconds) {
        Sound sound = soundCache.get(key);
        if (sound == null) {
            throw new IllegalArgumentException("Music with key '" + key + "' not found");
        }

        if (!isFormatCompatible(sound.getFormat(), defaultFormat)) {
            MetroLogger.logInfo("Warning: Music format " + sound.getFormat() +
                    " doesn't match output format " + defaultFormat);
        }

        musicPlayer.fadeTo(key, sound, targetVolume, fadeTimeSeconds);
    }
    public void playUISound(String key, float volume) {
        // Загружаем Clip, если ещё не загружен
        if (!uiClipCache.containsKey(key)) {
            preloadUIClip(key);
        }

        Clip clip = uiClipCache.get(key);
        if (clip == null) {
            MetroLogger.logWarning("UI Clip not found: " + key);
            return;
        }

        // Останавливаем, если уже играет
        if (clip.isRunning()) {
            clip.stop();
        }

        // Сбрасываем позицию
        clip.setFramePosition(0);

        try {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            // ✅ Учитываем sfxVolume + globalVolume
            float linearVolume = volume * sfxVolume * globalVolume;
            linearVolume = Math.max(0.0001f, Math.min(1.0f, linearVolume));
            float dB = (float) (Math.log(linearVolume) / Math.log(10.0) * 20.0);
            float minGain = gainControl.getMinimum();
            float maxGain = gainControl.getMaximum();
            dB = Math.max(minGain, Math.min(maxGain, dB));
            gainControl.setValue(dB);
        } catch (Exception e) {
            MetroLogger.logWarning("Cannot set volume for UI Clip: " + key);
        }
        // Запускаем
        clip.start();

    }
    public void playUISound(String key) {
        // Загружаем Clip, если ещё не загружен
        if (!uiClipCache.containsKey(key)) {
            preloadUIClip(key);
        }

        Clip clip = uiClipCache.get(key);
        if (clip == null) {
            MetroLogger.logWarning("UI Clip not found: " + key);
            return;
        }

        // Останавливаем, если уже играет
        if (clip.isRunning()) {
            clip.stop();
        }

        // Сбрасываем позицию
        clip.setFramePosition(0);

        try {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float linearVolume = GlobalSettings.getSfxVolume() * globalVolume;
            linearVolume = Math.max(0.0001f, Math.min(1.0f, linearVolume));
            float dB = (float) (Math.log(linearVolume) / Math.log(10.0) * 20.0);
            float minGain = gainControl.getMinimum();
            float maxGain = gainControl.getMaximum();
            dB = Math.max(minGain, Math.min(maxGain, dB));
            gainControl.setValue(dB);
        } catch (Exception e) {
            MetroLogger.logWarning("Cannot set volume for UI Clip: " + key);
        }
        // Запускаем
        clip.start();

    }
    public static SoundEngine getInstance() throws LineUnavailableException {
        if (instance == null) {
            instance = new SoundEngine();
        }
        return instance;
    }

    public Sound loadSound(String path, String key) throws IOException, JavaLayerException, UnsupportedAudioFileException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new IOException("File not found: " + path);
        }
        Sound sound = new Sound(is);
        soundCache.put(key, sound);

//        MetroLogger.logInfo("Sound loaded: " + key + ", format: " + sound.getFormat());
//        MetroLogger.logInfo("Data length: " + sound.getDataLength() + " bytes");

        return sound;
    }

    public void playSound(String key, float volume, boolean loop) {
        Sound sound = soundCache.get(key);
        if (sound == null) {
            throw new IllegalArgumentException("Sound with key '" + key + "' not found");
        }

        if (!isFormatCompatible(sound.getFormat(), defaultFormat)) {
            MetroLogger.logInfo("Warning: Sound format " + sound.getFormat() +
                    " doesn't match output format " + defaultFormat);
        }

        float finalVolume = volume * sfxVolume * globalVolume;

        // Создаем SoundSource
        SoundSource source = new SoundSource(sound, loop);
        source.setVolume(finalVolume);
        source.play();


        // Отправляем в аудио-менеджер
        audioMixer.playSoundSource(source, finalVolume, loop);
    }
    // В SoundEngine
    public void updateMusicVolume(float volume) {
        musicPlayer.setVolume(volume);
    }
    public void pauseMusic() {
        musicPlayer.pause();
    }

    public void resumeMusic() {
        musicPlayer.resume();
    }

    public void stopMusic() {
        musicPlayer.stop();
    }

    public boolean isMusicPlaying() {
        return musicPlayer.isPlaying();
    }

    public String getCurrentMusicKey() {
        return musicPlayer.getCurrentKey();
    }
    public void setGlobalVolume(float volume) {
        this.globalVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }
    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0.0f, Math.min(1.0f, volume));
        updateMusicVolume(1.0f); // применяем сразу, если музыка играет
    }

    public void setSfxVolume(float volume) {
        this.sfxVolume = Math.max(0.0f, Math.min(1.0f, volume));
        // Нет необходимости обновлять уже играющие SFX — они пересчитывают громкость при старте
    }

    public float getMusicVolume() { return musicVolume; }
    public float getSfxVolume() { return sfxVolume; }
    public void playSound(String key) {
        playSound(key, 1.0f, false);
    }

    public void playSound(String key, float volume) {
        playSound(key, volume, false);
    }

    public void playSoundLoop(String key) {
        playSound(key, 1.0f, true);
    }

    private boolean isFormatCompatible(AudioFormat source, AudioFormat target) {
        return source.getEncoding().equals(target.getEncoding()) &&
                source.getSampleSizeInBits() == target.getSampleSizeInBits() &&
                source.getChannels() == target.getChannels() &&
                Math.abs(source.getSampleRate() - target.getSampleRate()) < 1.0;
    }

    public void shutdown() {
        audioMixer.stop();

    }

    public int getPendingSounds() {
        return audioMixer.getQueueSize();
    }
    /**
     * Добавляет музыкальный трек в очередь воспроизведения.
     * Трек должен быть предзагружен через loadSound.
     */
    public void enqueueMusic(String key) {
        Sound sound = soundCache.get(key);
        if (sound == null) {
            MetroLogger.logError("Cannot enqueue music: track '" + key + "' not found in soundCache! Available keys: " + soundCache.keySet());
            return;
        }
        musicPlayer.enqueueTrack(key);
    }
    /**
     * Начинает воспроизведение очереди треков.
     * @param loop если true — очередь зацикливается бесконечно
     */
    public void playMusicQueue(boolean loop) {
        musicPlayer.startQueue(loop);
    }

    /**
     * Останавливает воспроизведение очереди (и текущий трек).
     */
    public void stopMusicQueue() {
        musicPlayer.stop();
    }
    private class MusicPlayer {
        private final AudioMixer audioMixer;

        private SoundSource currentSource;
        private String currentKey;
        private boolean isPaused = false;
        private java.util.Queue<String> trackQueue;
        private boolean isLoopQueue = false;
        private boolean isQueuePlaying = false;
        private boolean isManualControl = false; // если вызвали play/fadeTo вручную — отключаем очередь
        public MusicPlayer(AudioMixer mixer) {
            this.audioMixer = mixer;
            this.trackQueue = new java.util.LinkedList<>();  // ✅ ДОБАВЬ ЭТУ СТРОКУ
            this.originalQueue = new java.util.ArrayList<>(); // если не инициализировано — тоже добавь
        }


        // Новый метод: начать воспроизведение очереди
        public void startQueue(boolean loop) {
            if (trackQueue.isEmpty()) {
                MetroLogger.logWarning("Cannot start music queue: no tracks enqueued");
                return;
            }

            this.isLoopQueue = loop;
            this.isQueuePlaying = true;
            this.isManualControl = false;

            playNextInQueue();
        }
        private void playNextInQueue() {
            if (!isQueuePlaying) return;

            String nextKey = trackQueue.poll();
            if (nextKey == null) {
                if (isLoopQueue && !trackQueue.isEmpty()) {
                    // Очередь не пуста? Значит, мы просто очистили её — пересоздаём из кеша?
                    // Но лучше — сохранять оригинал. Давай сделаем отдельное поле для оригинальной очереди.
                    resetQueue();
                    nextKey = trackQueue.poll();
                } else {
                    isQueuePlaying = false;
                    return;
                }
            }

            if (nextKey == null) {
                isQueuePlaying = false;
                return;
            }

            Sound sound = SoundEngine.this.soundCache.get(nextKey);
            if (sound == null) {
                MetroLogger.logError("Track disappeared from cache: " + nextKey);
                playNextInQueue(); // попробуем следующий
                return;
            }

            // Плавный переход, если уже играет музыка
            if (currentSource != null && currentSource.isPlaying()) {
                String finalNextKey = nextKey;
                fadeOutCurrent(2.0f, () -> {
                    fadeInNew(finalNextKey, sound, 1.0f, 2.0f);
                    scheduleNextTrack(); // запланировать следующий после окончания
                });
            } else {
                play(nextKey, sound, 1.0f, false); // не зацикливаем отдельные треки — очередь сама зациклит
                scheduleNextTrack();
            }
        }

        // Восстанавливаем очередь из оригинала при зацикливании
        private java.util.List<String> originalQueue;

        public void enqueueTrack(String key) {
            if (!SoundEngine.this.soundCache.containsKey(key)) {
                MetroLogger.logWarning("Cannot enqueue music: track not found - " + key);
                return;
            }
            trackQueue.offer(key);
            originalQueue.add(key); // сохраняем для зацикливания
        }

        private void resetQueue() {
            trackQueue.clear();
            trackQueue.addAll(originalQueue);
        }

        // Планируем переход к следующему треку по окончании текущего
        private void scheduleNextTrack() {
            if (!isQueuePlaying) return;

            // Запускаем отслеживание окончания трека
            new Thread(() -> {
                if (currentSource == null) return;

                // Ждём, пока трек играет
                while (currentSource != null && currentSource.isPlaying()) {
                    try { Thread.sleep(500); } catch (InterruptedException e) { return; }
                }

                // Трек закончился — играем следующий
                if (isQueuePlaying && !isManualControl) {
                    playNextInQueue();
                }
            }).start();
        }
        public void play(String key, Sound sound, float volume, boolean loop) {
            stop(); // Останавливаем текущий, если есть
            isQueuePlaying = false;
            isManualControl = true;
            float finalVolume = volume * SoundEngine.this.musicVolume * SoundEngine.this.globalVolume;
            SoundSource source = new SoundSource(sound, loop);
            source.setVolume(finalVolume);
            source.play();

            this.currentSource = source;
            this.currentKey = key;
            this.isPaused = false;
            this.trackQueue = new java.util.LinkedList<>();
            audioMixer.playSoundSource(source, finalVolume, loop);
        }

        public void fadeTo(String key, Sound sound, float targetVolume, float fadeTimeSeconds) {
            isQueuePlaying = false;
            isManualControl = true;
            if (fadeTimeSeconds <= 0) {
                play(key, sound, targetVolume, true); // без фейда
                return;
            }

            // Если есть текущий трек — запускаем fade out
            if (currentSource != null && currentSource.isPlaying()) {
                fadeOutCurrent(fadeTimeSeconds, () -> {
                    // После fade out — запускаем новый трек с fade in
                    fadeInNew(key, sound, targetVolume, fadeTimeSeconds);
                });
            } else {
                // Нет текущего — просто запускаем с fade in
                fadeInNew(key, sound, targetVolume, fadeTimeSeconds);
            }
        }

        private void fadeOutCurrent(float fadeTimeSeconds, Runnable onComplete) {
            if (currentSource == null) {
                if (onComplete != null) onComplete.run();
                return;
            }

            new Thread(() -> {
                float startVolume = currentSource.getVolume();
                long startTime = System.currentTimeMillis();
                long fadeDuration = (long) (fadeTimeSeconds * 1000);

                while (System.currentTimeMillis() - startTime < fadeDuration) {
                    if (currentSource == null || !currentSource.isPlaying()) break;

                    float elapsed = (float) (System.currentTimeMillis() - startTime) / fadeDuration;
                    float newVol = startVolume * (1f - elapsed);
                    currentSource.setVolume(newVol);

                    try { Thread.sleep(20); } catch (InterruptedException e) { break; }
                }

                if (currentSource != null) {
                    currentSource.stop();
                }

                if (onComplete != null) onComplete.run();
            }).start();
        }

        private void fadeInNew(String key, Sound sound, float targetVolume, float fadeTimeSeconds) {
            float finalTargetVolume = targetVolume * SoundEngine.this.musicVolume * SoundEngine.this.globalVolume;
            SoundSource source = new SoundSource(sound, true); // loop = true для фона
            source.setVolume(0f); // начинаем с 0
            source.play();

            this.currentSource = source;
            this.currentKey = key;
            this.isPaused = false;

            audioMixer.playSoundSource(source, 0f, true);

            new Thread(() -> {
                float startVolume = 0f;
                long startTime = System.currentTimeMillis();
                long fadeDuration = (long) (fadeTimeSeconds * 1000);

                while (System.currentTimeMillis() - startTime < fadeDuration) {
                    if (currentSource == null || !currentSource.isPlaying()) break;

                    float elapsed = (float) (System.currentTimeMillis() - startTime) / fadeDuration;
                    float newVol = startVolume + (finalTargetVolume * elapsed);
                    currentSource.setVolume(newVol);

                    try { Thread.sleep(20); } catch (InterruptedException e) { break; }
                }

                if (currentSource != null) {
                    currentSource.setVolume(finalTargetVolume); // на всякий случай
                }
            }).start();
        }

        public void pause() {
            if (currentSource != null && currentSource.isPlaying()) {
                currentSource.pause();
                isPaused = true;
            }
        }

        public void resume() {
            if (currentSource != null && isPaused) {
                currentSource.resume();
                isPaused = false;
            }
        }

        public void stop() {
            if (currentSource != null) {
                currentSource.stop();
                currentSource = null;
                currentKey = null;
                isPaused = false;
            }
        }

        public boolean isPlaying() {
            return currentSource != null && currentSource.isPlaying();
        }

        public String getCurrentKey() {
            return currentKey;
        }

        public void setVolume(float volume) {
            if (currentSource != null) {
                float finalVol = volume * SoundEngine.this.musicVolume * SoundEngine.this.globalVolume;
                currentSource.setVolume(finalVol);
            }
        }
    }
}