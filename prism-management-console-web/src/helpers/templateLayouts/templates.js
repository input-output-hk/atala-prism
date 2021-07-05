import { template0 } from './template0';
import { template1 } from './template1';
import { template2 } from './template2';
import { template3 } from './template3';
import { template4 } from './template4';

import TemplateLayoutImage0 from '../../images/TemplateLayout_0.svg';
import TemplateLayoutImage1 from '../../images/TemplateLayout_1.svg';
import TemplateLayoutImage2 from '../../images/TemplateLayout_2.svg';
import TemplateLayoutImage3 from '../../images/TemplateLayout_3.svg';
import TemplateLayoutImage4 from '../../images/TemplateLayout_4.svg';

export const templateLayouts = [
  {
    thumb: TemplateLayoutImage0,
    images: ['userIcon'],
    ...template0
  },
  {
    thumb: TemplateLayoutImage1,
    images: ['companyIcon'],

    ...template1
  },
  {
    thumb: TemplateLayoutImage2,
    images: ['companyIcon'],
    ...template2
  },
  {
    thumb: TemplateLayoutImage3,
    images: ['companyIcon'],
    ...template3
  },
  {
    thumb: TemplateLayoutImage4,
    images: ['companyIcon', 'userIcon'],
    ...template4
  }
];
