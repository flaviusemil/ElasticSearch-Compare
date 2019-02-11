package com.swisscom.allegro.escomparison;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
class Importers implements Serializable {

    private int custNo;
    private int cbsNo;
    private int soiNo;
    private int qpiNo;
    private int itemNo;

}
