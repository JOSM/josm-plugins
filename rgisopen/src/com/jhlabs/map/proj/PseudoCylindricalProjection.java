/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

/**
 * Bernhard Jenny, May 18 2010:
 * Changed base class from CylindricalProjection to Projection, removed
 * isRectilinear method, changed description of class.
 * 23 September 2010: added parallelsAreParallel
 */
package com.jhlabs.map.proj;

/**
 * The superclass for all pseudo-cylindrical projections - eg. sinusoidal
 * These are projections where parallels are straight and parallel, and
 * meridians are curved.
 */
public abstract class PseudoCylindricalProjection extends Projection {

    public String toString() {
        return "Pseudo-Cylindrical";
    }

    public boolean parallelsAreParallel() {
        return true;
    }
}
