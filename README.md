# android-slidr

Another android slider / seekbar, but different :-) 

# Download

[ ![Download](https://api.bintray.com/packages/florent37/maven/android-slidr/images/download.svg) ](https://bintray.com/florent37/maven/android-slidr/_latestVersion)
```java
dependencies {
    compile 'com.github.florent37:android-slidr:1.0.4'
}
```

[![png](https://raw.githubusercontent.com/florent37/android-slidr/master/medias/slidr1.png)](https://github.com/florent37/android-slidr)

```xml
<com.github.florent37.androidslidr.Slidr
        android:layout_width="250dp"
        android:layout_height="wrap_content"
        app:slidr_regions_centerText="false"
        app:slidr_region_leftColor="#4CAF50" />
```

# Step

[![png](https://raw.githubusercontent.com/florent37/android-slidr/master/medias/slidr2_1.png)](https://github.com/florent37/android-slidr)
[![png](https://raw.githubusercontent.com/florent37/android-slidr/master/medias/slidr2_2.png)](https://github.com/florent37/android-slidr)

```xml
<com.github.florent37.androidslidr.Slidr
       android:id="@+id/slidr"
       android:layout_width="250dp"
       android:layout_height="wrap_content"
       app:slidr_region_leftColor="#4CAF50"
       app:slidr_step_colorizeAfterLast="true" />
```

```java
final Slidr slidr = (Slidr) findViewById(R.id.slideure);
slidr.setMax(500);
slidr.addStep(new Slidr.Step("test", 250, Color.parseColor("#007E90"), Color.RED));
slidr.setTextMax("max\nvalue");
slidr.setCurrentValue(300);
slidr.setListener(new Slidr.Listener() {
    @Override
    public void valueChanged(Slidr slidr, float currentValue) {

    }

    @Override
    public void bubbleClicked(Slidr slidr) {

    }
});
```

# Region

[![png](https://raw.githubusercontent.com/florent37/android-slidr/master/medias/slidr_region.png)](https://github.com/florent37/android-slidr)

```xml
<com.github.florent37.androidslidr.Slidr
       android:id="@+id/slideure_regions"
       android:layout_margin="20dp"
       android:layout_width="250dp"
       android:layout_height="wrap_content"
       app:slidr_regions="true"
       app:slidr_region_leftColor="@color/colorLeft"
       app:slidr_region_rightColor="@color/colorRight"
       app:slidr_regions_textFollowRegionColor="true"
       app:slidr_regions_centerText="true"
       app:slidr_draw_bubble="false"
       />
```

```java
final Slidr slidr = (Slidr) findViewById(R.id.slideure_regions);
slidr.setMax(3000);
slidr.setRegionTextFormatter(new Slidr.RegionTextFormatter() {
    @Override
    public String format(int region, float value) {
        return String.format("region %d : %d", region, (int) value);
    }
});
slidr.addStep(new Slidr.Step("test", 1500, Color.parseColor("#007E90"), Color.parseColor("#111111")));
```


# Credits

Author: Florent Champigny [http://www.florentchampigny.com/](http://www.florentchampigny.com/)

Blog : [http://www.tutos-android-france.com/](http://www.www.tutos-android-france.com/)

<a href="https://plus.google.com/+florentchampigny">
  <img alt="Follow me on Google+"
       src="https://raw.githubusercontent.com/florent37/DaVinci/master/mobile/src/main/res/drawable-hdpi/gplus.png" />
</a>
<a href="https://twitter.com/florent_champ">
  <img alt="Follow me on Twitter"
       src="https://raw.githubusercontent.com/florent37/DaVinci/master/mobile/src/main/res/drawable-hdpi/twitter.png" />
</a>
<a href="https://www.linkedin.com/in/florentchampigny">
  <img alt="Follow me on LinkedIn"
       src="https://raw.githubusercontent.com/florent37/DaVinci/master/mobile/src/main/res/drawable-hdpi/linkedin.png" />
</a>


License
--------

    Copyright 2017 Florent37, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
