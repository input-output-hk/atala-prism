export const georgiaCoursesView = {
  html: `<div
    style="background-color: #FFF6CF; font-size: 13.5px; color: #1A253A; width: 100%; padding: 10px 10px 10px 1.5em;">
    {{courseName}}
  </div>
  <div style="display: flex; padding: 1em 1.5em 1.5em 1.5em; width: 100%; justify-content: space-between;">
    <div style="flex-direction: column; width:110px;">
      <p style="font-size: 9px; color: #828282; margin: 0.53em 0 1.5em; text-transform: uppercase;">
        Course Code
      </p>
      <h3 style="color: #3C393A; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
        {{courseCode}}
      </h3>
    </div>
    <div style="flex-direction: column;">
      <p style="font-size: 9px; color: #828282; margin: 0.53em 0 1.5em; text-transform: uppercase;">Credits</p>
      <h3 style="color: #3C393A; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
        {{credits}}
      </h3>
    </div>
    <div style="flex-direction: column;">
      <p style="font-size: 9px; color: #828282; margin: 0.53em 0 1.5em; text-transform: uppercase;">Score</p>
      <h3 style="color: #3C393A; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
        {{score}}
      </h3>
    </div>
    <div style="flex-direction: column;">
      <p style="font-size: 9px; color: #828282; margin: 0.53em 0 1.5em; text-transform: uppercase;">Grade</p>
      <h3 style="color: #3C393A; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
        {{grade}}
      </h3>
    </div>
  </div>`,
  placeholders: {
    courseName: '{{courseName}}',
    courseCode: '{{courseCode}}',
    credits: '{{credits}}',
    score: '{{score}}',
    grade: '{{grade}}'
  }
};

export const ethiopiaCoursesView = {
  html: `<div
    style="background-color: #FFCDD0; font-size: 13.5px; color: #1A253A; width: 100%; padding: 10px 10px 10px 1.5em;">
    {{courseName}}
  </div>
  <div style="display: flex; padding: 1em 1.5em 1.5em 1.5em; width: 100%; justify-content: space-between;">
    <div style="flex-direction: column; width:110px;">
      <p style="font-size: 9px; color: #828282; margin: 0.53em 0 1.5em; text-transform: uppercase;">
        Course Code
      </p>
      <h3 style="color: #3C393A; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
        {{courseCode}}
      </h3>
    </div>
    <div style="flex-direction: column;">
      <p style="font-size: 9px; color: #828282; margin: 0.53em 0 1.5em; text-transform: uppercase;">Credits</p>
      <h3 style="color: #3C393A; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
        {{credits}}
      </h3>
    </div>
    <div style="flex-direction: column;">
      <p style="font-size: 9px; color: #828282; margin: 0.53em 0 1.5em; text-transform: uppercase;">Score</p>
      <h3 style="color: #3C393A; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
        {{score}}
      </h3>
    </div>
    <div style="flex-direction: column;">
      <p style="font-size: 9px; color: #828282; margin: 0.53em 0 1.5em; text-transform: uppercase;">Grade</p>
      <h3 style="color: #3C393A; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
        {{grade}}
      </h3>
    </div>
  </div>`,
  placeholders: {
    courseName: '{{courseName}}',
    courseCode: '{{courseCode}}',
    credits: '{{credits}}',
    score: '{{score}}',
    grade: '{{grade}}'
  }
};
