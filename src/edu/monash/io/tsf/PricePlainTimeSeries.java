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
 * 用来存放电价数据的类（自己创建的）
 * @see WeatherPlainTimeSeries (用来存放天气数据的类)
 */
public class PricePlainTimeSeries {

    private final List<ZonedDateTime> instant;
    private final List<String[]> data;


    public PricePlainTimeSeries(List<ZonedDateTime> instant, List<String[]> data) {

        this.instant = new ArrayList<>(instant);
        this.data = new ArrayList<>(data);

        if (this.instant.size() != this.data.size())
            throw new IllegalArgumentException("Unequal length timestamps and data.");
    }


    /**
     * create by 徐耀建
     * @return
     */
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

    public PricePlainTimeSeries forMonth(int year, Month month) {

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

        return new PricePlainTimeSeries(instant, data);
    }

    public PricePlainTimeSeries withZoneSameInstant(ZoneId newTZ) {

        List<ZonedDateTime> instant = new ArrayList<>();

        for (ZonedDateTime original : this.instant)
            instant.add(original.withZoneSameInstant(newTZ));

        return new PricePlainTimeSeries(instant, data);
    }

    public PricePlainTimeSeries minus(TemporalAmount delta) {

        List<ZonedDateTime> instant = new ArrayList<>();

        for (ZonedDateTime original : this.instant)
            instant.add(original.minus(delta));

        return new PricePlainTimeSeries(instant, data);
    }

    public PricePlainTimeSeries fillForward(TemporalAmount delta) {

        List<ZonedDateTime> instant = new ArrayList<>();
        List<String[]> data = new ArrayList<>();

        for (int i = 0; i < this.instant.size(); i++) {

            ZonedDateTime original = this.instant.get(i);
            String[] datum = this.data.get(i);

            instant.add(original);
            data.add(datum);
            instant.add(original.plus(delta));
            data.add(datum);
        }

        return new PricePlainTimeSeries(instant, data);
    }

    public static PricePlainTimeSeries fromRows(List<String[]> rows, int timeColumn, DateTimeFormatter format, ZoneId tz) {

        List<ZonedDateTime> instant = new ArrayList<>();
        List<String[]> data = new ArrayList<>();

        for (String[] row : rows) {
            instant.add(LocalDateTime.parse(row[timeColumn], format).atZone(tz));
            data.add(row);
        }
        return new PricePlainTimeSeries(instant, data);
    }

}
