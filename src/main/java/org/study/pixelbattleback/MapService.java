package org.study.pixelbattleback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.study.pixelbattleback.dto.Map;
import org.study.pixelbattleback.dto.PixelRequest;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class MapService {
    private static final Logger logger = LoggerFactory.getLogger(MapService.class);

    public static final String MAP_BIN = "map.bin";

    private final int width;

    private final int height;

    private final int[] colors;
    private final ReentrantReadWriteLock[] locks;

    private boolean isChanged;

    /**
     * Пытаемся загрузить карту из файла на старте, или же начинаем с пустой карты
     */
    public MapService() {
        Map tmp = new Map();
        tmp.setWidth(100);
        tmp.setHeight(100);
        tmp.setColors(new int[tmp.getWidth() * tmp.getHeight()]);
        try (FileInputStream fileInputStream = new FileInputStream(MAP_BIN);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            Object o = objectInputStream.readObject();
            tmp = (Map) o;
        } catch (Exception e) {
            logger.error("Загрузка не удалась, начинаем с пустой карты. " + e.getMessage(), e);
        }
        width = tmp.getWidth();
        height = tmp.getHeight();
        colors = tmp.getColors();
        locks = new ReentrantReadWriteLock[width * height];
    }

    /**
     * Окрашивание пикселя
     *
     * @param pixel
     * @return
     */
    public boolean draw(PixelRequest pixel) {
        int x = pixel.getX();
        int y = pixel.getY();
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }
        var index = y * width + x;
        Lock lock = getWriteLock(index);
        lock.lock();
        colors[index] = pixel.getColor();
        lock.unlock();
        isChanged = true;
        return true;
    }

    /**
     * Чтение всей карты
     *
     * @return
     */
    private int[] getColors() {
        lockWriting();
        var result = Arrays.copyOf(colors, colors.length);
        unlockWriting();
        return result;
    }

    private void lockWriting() {
        for (int i = 0; i < locks.length; i++) {
            getReadLock(i).lock();
        }
    }

    private void unlockWriting() {
        for (int i = 0; i < locks.length; i++) {
            getReadLock(i).unlock();
        }
    }

    public Map getMap() {
        Map mapObj = new Map();
        mapObj.setColors(getColors());
        mapObj.setWidth(width);
        mapObj.setHeight(height);
        return mapObj;
    }

    private ReentrantReadWriteLock.WriteLock getWriteLock(int index) {
        if (locks[index] == null) {
            locks[index] = new ReentrantReadWriteLock();
        }
        return locks[index].writeLock();
    }

    private ReentrantReadWriteLock.ReadLock getReadLock(int index) {
        if (locks[index] == null) {
            locks[index] = new ReentrantReadWriteLock();
        }
        return locks[index].readLock();
    }

    /**
     * Периодически сохраняем карту в файл
     */
    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.SECONDS)
    public synchronized void writeToFile() {
        if (!isChanged) {
            return;
        }
        isChanged = false;
        try (FileOutputStream fileOutputStream = new FileOutputStream(MAP_BIN);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeObject(getMap());
            logger.info("Карта сохранена в файле {}", MAP_BIN);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }


}
