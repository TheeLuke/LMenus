package io.github.theeluke.managers;

import io.github.theeluke.models.Menu;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MenuManager {

    private final Map<String, Menu> menus = new HashMap<>();

    public void addMenu(Menu menu) {
        menus.put(menu.getName().toLowerCase(), menu);
    }

    public void removeMenu(String name) {
        menus.remove(name.toLowerCase());
    }

    public Menu getMenu(String name) {
        return menus.get(name.toLowerCase());
    }

    public boolean menuExists(String name) {
        return menus.containsKey(name.toLowerCase());
    }

    public Collection<Menu> getLoadedMenus() {
        return menus.values();
    }

    public void clear() {
        menus.clear();
    }
}
