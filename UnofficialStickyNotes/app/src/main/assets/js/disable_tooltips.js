javascript:(function() {
        /* Copyright (C) 2020  Andrew Larson (thealiendrew@gmail.com)
         *
         * This program is free software: you can redistribute it and/or modify
         * it under the terms of the GNU General Public License as published by
         * the Free Software Foundation, either version 3 of the License, or
         * (at your option) any later version.
         *
         * This program is distributed in the hope that it will be useful,
         * but WITHOUT ANY WARRANTY; without even the implied warranty of
         * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
         * GNU General Public License for more details.
         *
         * You should have received a copy of the GNU General Public License
         * along with this program.  If not, see <https://www.gnu.org/licenses/>.
         */
        console.log("disable_tooltips.js started");

        var disableCustomToolTips = document.createElement('style');
        disableCustomToolTips.type = 'text/css';
        disableCustomToolTips.innerHTML = '.ms-Tooltip{display:none!important}';
        document.head.appendChild(disableCustomToolTips);

        setInterval(function() {
            var elementsWithTitle = document.querySelectorAll('[title]');
            if (typeof(elementsWithTitle) != 'undefined' && elementsWithTitle != null && elementsWithTitle.length > 0) {
                 elementsWithTitle.forEach((element) => {
                     element.title = ' ';
                 });
            }
        }, 100);

    })()