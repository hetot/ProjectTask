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
import java.util.concurrent.atomic.AtomicIntegerArray;

@Service
public class MapService {
    private static final Logger logger = LoggerFactory.getLogger(MapService.class);

    public static final String MAP_BIN = "map.bin";

    private final int width;

    private final int height;

    private final AtomicIntegerArray atomicColors;

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
        var colors = tmp.getColors();
        atomicColors = new AtomicIntegerArray(colors);
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
        int index = y * width + x;
        atomicColors.set(index, pixel.getColor());
        isChanged = true;
        return true;
    }

    /**
     * Чтение всей карты
     *
     * @return
     */
    private int[] getColors() {
        int[] colors = new int[width * height];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = atomicColors.get(i);
        }
        return colors;
    }

    public Map getMap() {
        Map mapObj = new Map();
        mapObj.setColors(getColors());
        mapObj.setWidth(width);
        mapObj.setHeight(height);
        return mapObj;
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
