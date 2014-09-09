/*
 * android-spinnerwheel
 * https://github.com/ai212983/android-spinnerwheel
 *
 * based on
 *
 * Android Wheel Control.
 * https://code.google.com/p/android-wheel/
 *
 * Copyright 2011 Yuri Kanivets
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package antistatic.spinnerwheel.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * The simple Array spinnerwheel adapter, able to hold two TextView references
 * for somewhat more complex wheel items that want to store information in two separate TextViews.
 * @param <T> the element type
 */
public class ArrayWheelAdapter2<T> extends AbstractWheelTextAdapter {
    
    // items
	private T[] items;
    private T[] items2;
    
    private int itemTextResource2Id;

    /**
     * Constructor
     * @param context the current context
     * @param items the items for the first TextView
     * @param items2 the items for the second TextView
     */
    public ArrayWheelAdapter2(Context context, T[] items, T[] items2) {
        super(context);
        
        this.items = items;
        this.items2 = items2;
    }
    
    public void setItems(T[] items, T[] items2) {
    	this.items = items;
    	this.items2 = items2;
    	notifyDataChangedEvent();
    }
    
    /**
     * Gets resource Id for second text view in item layout 
     * @return the item text resource Id
     */
    public int getItemTextResource2() {
        return itemTextResource2Id;
    }
    
    /**
     * Sets resource Id for second text view in item layout 
     * @param itemTextResourceId the item text resource Id to set
     */
    public void setItemTextResource2(int itemTextResourceId) {
        this.itemTextResource2Id = itemTextResourceId;
    }
    
    @Override
    public CharSequence getItemText(int index) {
        if (index >= 0 && index < items.length) {
            T item = items[index];
            if (item instanceof CharSequence) {
                return (CharSequence) item;
            }
            return item.toString();
        }
        return null;
    }
    
    public CharSequence getItemText2(int index) {
        if (index >= 0 && index < items2.length) {
            T item = items2[index];
            if (item instanceof CharSequence) {
                return (CharSequence) item;
            }
            return item.toString();
        }
        return null;
    }

    @Override
    public View getItem(int index, View convertView, ViewGroup parent) {
        if (index >= 0 && index < getItemsCount()) {
            if (convertView == null) {
                convertView = getView(itemResourceId, parent);
            }
            TextView textView = getTextView(convertView, itemTextResourceId);
            TextView textView2 = getTextView(convertView, itemTextResource2Id);
            
            if (textView != null) {
                CharSequence text = getItemText(index);
                if (text == null) {
                    text = "";
                }
                textView.setText(text);
                configureTextView(textView);
            }
            
            if (textView2 != null) {
                CharSequence text = getItemText2(index);
                if (text == null) {
                    text = "";
                }
                textView2.setText(text);
                configureTextView(textView2);
            }
            return convertView;
        }
        return null;
    }

    @Override
    public int getItemsCount() {
        return items.length;
    }
}
