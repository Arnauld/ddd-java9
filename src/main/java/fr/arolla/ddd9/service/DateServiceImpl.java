package fr.arolla.ddd9.service;

import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.GregorianCalendar;

@Service
public class DateServiceImpl implements DateService {

    private Calendar forcedCalendar;

    @Override
    public Calendar getCurrentDate() {
        return (forcedCalendar != null ? forcedCalendar : new GregorianCalendar());
    }

    @Override
    public void forceCalendar(Calendar now) {
        this.forcedCalendar = now;
    }
}