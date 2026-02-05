package metroline.objects.gameobjects;

import metroline.objects.enums.StationColors;
import metroline.util.localizate.LngUtil;

import java.awt.*;
import java.util.concurrent.TimeUnit;

// FIX Пересмотреть константы множителя дохода поездов.
public class GameConstants {



    public static final String[] NAME_PARTS = {
            LngUtil.translatable("st_name_1"),
            LngUtil.translatable("st_name_2"),
            LngUtil.translatable("st_name_3"),
            LngUtil.translatable("st_name_4"),
            LngUtil.translatable("st_name_5"),
            LngUtil.translatable("st_name_6"),
            LngUtil.translatable("st_name_7"),
            LngUtil.translatable("st_name_8"),
            LngUtil.translatable("st_name_9"),
            LngUtil.translatable("st_name_10"),
            LngUtil.translatable("st_name_11"),
            LngUtil.translatable("st_name_12"),
            LngUtil.translatable("st_name_13"),
            LngUtil.translatable("st_name_14"),
            LngUtil.translatable("st_name_15"),
            LngUtil.translatable("st_name_16"),
            LngUtil.translatable("st_name_17"),
            LngUtil.translatable("st_name_18"),
            LngUtil.translatable("st_name_19"),
            LngUtil.translatable("st_name_20"),
            LngUtil.translatable("st_name_21"),
            LngUtil.translatable("st_name_22"),
            LngUtil.translatable("st_name_23"),
            LngUtil.translatable("st_name_24"),
            LngUtil.translatable("st_name_25"),
    };

    public static final Color[] COLORS = StationColors.getAllColors();

    public static final Color[] COLORS_DARK = {
            // Приглушенные, землистые тона
            new Color(160, 60, 50),    // Терракотовый
            new Color(80, 110, 60),     // Оливковый
            new Color(60, 100, 130),    // Приглушенный синий
            new Color(70, 80, 160),     // Сдержанный синий
            new Color(80, 140, 160),    // Морской
            new Color(120, 80, 50),     // Коричневый
            new Color(170, 100, 50),    // Тыквенный
            new Color(130, 70, 130),    // Приглушенный фиолетовый
            new Color(160, 140, 50),    // Горчичный
            new Color(140, 140, 140),   // Серый
            new Color(120, 150, 70),    // Оливково-зеленый
            new Color(70, 120, 120),    // Бирюзовый
            new Color(150, 80, 110),    // Приглушенный розовый
            new Color(50, 110, 90),     // Изумрудный
            new Color(150, 70, 80),     // Вишневый
            new Color(100, 130, 100)    // Мятный
    };
}
