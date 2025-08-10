package game.objects;

import util.LngUtil;

import java.awt.*;

public class GameConstants {
    public static final int RENDER_LAYER_TUNNELS = 0;
    public static final int RENDER_LAYER_TRANSFER = 1;
    public static final int RENDER_LAYER_STATIONS = 2;
    public static final int RENDER_LAYER_SELECTION = 3;
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

    public static final Color[] COLORS = {
            new Color(157, 6, 6),
            new Color(0, 100, 0),
            new Color(0, 120, 190),
            new Color(27, 57, 208),
            new Color(25, 149, 176),
            new Color(110, 63, 21),
            new Color(200, 100, 0),
            new Color(133, 7, 133),
            new Color(211, 179, 8),
            new Color(153, 153, 153),
            new Color(153, 204, 0),
            new Color(79, 155, 155),
            new Color(201, 48, 128),
            new Color(3, 121, 95),
            new Color(148, 21, 73),
            new Color(109, 148, 104),
    };
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
