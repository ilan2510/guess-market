package guessmarket.engine;

import java.util.List;

class LoadResult
{

    private final List<Event> events;
    private final List<User> users;

    LoadResult(List<Event> events, List<User> users)
    {
        this.events = events;
        this.users = users;
    }

    List<Event> getEvents()
    {
        return events;
    }

    List<User> getUsers()
    {
        return users;
    }
}
