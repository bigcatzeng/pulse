package com.trxs.commons.util;

import java.util.Observer;

public interface Observerable
{
    public void registerObserver(Observer o);
    public void removeObserver(Observer o);
    public void notifyObserver();
}
