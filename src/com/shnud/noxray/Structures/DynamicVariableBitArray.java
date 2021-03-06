package com.shnud.noxray.Structures;

/**
 * Created by Andrew on 02/01/2014.
 */
public class DynamicVariableBitArray extends VariableBitArray {

    public DynamicVariableBitArray(int bitsPerVal, DynamicByteArray array) {
        super(bitsPerVal, array);
    }

    public DynamicVariableBitArray(int bitsPerVal, int size) {
        this(bitsPerVal, new DynamicByteArray(sizeRequiredFor(size, bitsPerVal)));
    }

    public DynamicByteArray getByteArray() {
        return (DynamicByteArray) super.getByteArray();
    }

    public void convertTo(int bitsVerVal) {
        byte[] newArray = convertWrappedByteArray(getByteArray().getPrimitiveByteArray(), getBitsPerVal(), bitsVerVal);
        setByteArray(new DynamicByteArray(newArray));
        setBitsPerVal(bitsVerVal);
    }
}
