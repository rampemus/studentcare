package org.utu.studentcare.applogic;

/**
 * Maybe useful routines for visualizing progress of tasks.
  */
// TODO: Oheisluokka, voi käyttää tai käyttää jotain JavaFX:n omia
public class TaskStuff {
    private final Object lock = new Object();
    private int totalSteps;
    private int currentStep;
    private String status;

    public int currentStep() {
        synchronized (lock) {
            return currentStep;
        }
    }

    public int totalSteps() {
        synchronized (lock) {
            return totalSteps;
        }
    }

    public double progress() {
        synchronized (lock) {
            return totalSteps == 0 ? 1 : 1.0 * currentStep() / totalSteps;
        }
    }

    public String status() {
        synchronized (lock) {
            return status;
        }
    }

    public void taskStarted(String taskName, int steps) {
        synchronized (lock) {
            currentStep = 0;
            totalSteps = steps;
            status = taskName + " started.";
        }
        System.out.print(taskName);
    }

    public void taskUpdate(String status) {
        synchronized (lock) {
            taskUpdate();
            this.status = status;
        }
    }

    public void taskUpdate() {
        synchronized (lock) {
            currentStep++;
            if (totalSteps < 10 || (currentStep % (totalSteps / 10) == 0))
                System.out.print(".");
        }
    }

    public void taskFinished(String taskName) {
        synchronized (lock) {
            currentStep = totalSteps;
            status = taskName + " finished.";
            System.out.println();
        }
    }
}