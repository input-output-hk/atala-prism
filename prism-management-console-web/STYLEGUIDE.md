# Style Guide

This is a minimal style guide intended to maintain consistency of code elements 
that might not be enforced with tools like Eslint and Prettier. 

## Table of contents

* [Unused parameters and variables](#unused-parameters-and-variables)
* [Component events](#component-events)

## Unused parameters and variables
In some rare situations it would be actually convenient to have unused variables left in the code. One of such cases is unused variables when destructuring arrays.

Unused variables should be prefixed with `_`, for example: 

`const [_prefix, iconData] = values.icon.split(',');`

Following ESLint rule should be in place to support it:

`"no-unused-vars": ["warn", { "varsIgnorePattern": "^_" }]`

## Component events

Component props which are handlers (or a function in general), should be treated as events and follow its naming convention, for example:

`onClick`, `onChange`, `onSomethingChange`, `onSort`, etc.

Bad:

```js
<CustomDateRangePicker setStartDate={setStartDate} setEndDate={setEndDate} />
```

Good:

```js
<CustomDateRangePicker onStartDateChange={handleStartDateChange} onEndDateChange={setEndDate} />
```

or even:

```js
<CustomDateRangePicker onChange={({startDate, endDate}) => {/* handle dates change */} } />
```
