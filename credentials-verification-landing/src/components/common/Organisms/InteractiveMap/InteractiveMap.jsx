import React, { Component } from 'react';
import { AtalaPrismDemo } from 'atala-prism-demo';

class InteractiveMap extends Component {
  constructor(props) {
    super(props);
    this.setMapStep = this.setMapStep.bind(this);
  }

  componentDidMount() {
    window.addEventListener('click', () => {
        this.setMapStep();
    });
  }

  componentDidUpdate() {
    this.setMapStep();
  }

  setMapStep() {
    const { mapStep } = this.props;
    this.refs.demo.setStep(mapStep);
  }

  render() {
    return (
      <div>
        <AtalaPrismDemo ref="demo" />
      </div>
    );
  }
}

export default InteractiveMap;
