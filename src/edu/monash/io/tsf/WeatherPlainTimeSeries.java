package edu.monash.io.tsf;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;

/**
 * 用来存放天气数据的类（自己创建的）
 * @see PricePlainTimeSeries (用来存放价格数据的类)
 */
public class WeatherPlainTimeSeries {

    private final List<ZonedDateTime> instant;
    private final List<String[]> data;


    public WeatherPlainTimeSeries(List<ZonedDateTime> instant, List<String[]> data) {

        this.instant = new ArrayList<>(instant);
        this.data = new ArrayList<>(data);

        if (this.instant.size() != this.data.size())
            throw new IllegalArgumentException("Unequal length timestamps and data.");
    }


    public List<ZonedDateTime> getInstant() {
        return instant;
    }

    public int size() {
        return instant.size();
    }

    public List<String[]> getData() {
        return data;
    }

    public List<ZonedDateTime> getTimestamps() {
        return instant;
    }

    public WeatherPlainTimeSeries forMonth(int year, Month month) {

        List<ZonedDateTime> instant = new ArrayList<>();
        List<String[]> data = new ArrayList<>();

        for (int i = 0; i < this.instant.size(); i++) {

            ZonedDateTime original = this.instant.get(i);
            String[] datum = this.data.get(i);

            if (original.getYear() == year && original.getMonth().equals(month)) {
                instant.add(original);
                data.add(datum);
            }
        }

        return new WeatherPlainTimeSeries(instant, data);
    }

    public WeatherPlainTimeSeries withZoneSameInstant(ZoneId newTZ) {

        List<ZonedDateTime> instant = new ArrayList<>();

        for (ZonedDateTime original : this.instant)
            instant.add(original.withZoneSameInstant(newTZ));

        return new WeatherPlainTimeSeries(instant, data);
    }

    public WeatherPlainTimeSeries minus(TemporalAmount delta) {

        List<ZonedDateTime> instant = new ArrayList<>();

        for (ZonedDateTime original : this.instant)
            instant.add(original.minus(delta));

        return new WeatherPlainTimeSeries(instant, data);
    }

    public WeatherPlainTimeSeries fillForward(TemporalAmount delta) {

        List<ZonedDateTime> instant = new ArrayList<>();
        List<String[]> data = new ArrayList<>();

        for (int i = 0; i < this.instant.size(); i++) {

            ZonedDateTime original = this.instant.get(i);
            String[] datum = this.data.get(i);

            instant.add(original);
            data.add(datum);

            ZonedDateTime plus = original.plus(delta);
            instant.add(plus);
            data.add(datum);

            ZonedDateTime plus1 = plus.plus(delta);
            instant.add(plus1);
            data.add(datum);

            ZonedDateTime plus2 = plus1.plus(delta);
            instant.add(plus2);
            data.add(datum);
        }

        return new WeatherPlainTimeSeries(instant, data);
    }

    public static WeatherPlainTimeSeries fromRows(List<String[]> rows, int timeColumn,  DateTimeFormatter format, ZoneId tz) {

        List<ZonedDateTime> instant = new ArrayList<>();
        List<String[]> data = new ArrayList<>();

        for (String[] row : rows) {
            instant.add(LocalDateTime.parse(row[timeColumn], format).atZone(tz));
            data.add(row);
        }

        return new WeatherPlainTimeSeries(instant, data);
    }

}
