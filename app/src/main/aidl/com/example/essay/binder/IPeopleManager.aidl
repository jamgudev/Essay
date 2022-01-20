// IPeopleManager.aidl
package com.example.essay.binder;

// Declare any non-default types here with import statements
import com.example.essay.binder.Person;

interface IPeopleManager {

    void addPerson(in Person person);
    List<Person> getPeople();

}
