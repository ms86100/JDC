export default function IntroductionGadget() {
  return (
    <section className="jdc-gadget" aria-label="Introduction">
      <div className="jdc-gadget-header">
        <span>Introduction</span>
      </div>
      <div className="jdc-gadget-body">
        <h3 style={{ marginTop: 0 }}>Welcome to Systems</h3>
        <p>
          New to Systems? Check out the{' '}
          <a href="https://docs.avisys.example.com/guides" target="_blank" rel="noreferrer">
            Systems 101 guide
          </a>{' '}
          or our{' '}
          <a href="https://www.atlassian.com/university" target="_blank" rel="noreferrer">
            training course
          </a>
          .
        </p>
        <p style={{ fontSize: 12, color: 'var(--jdc-text-subtle)' }}>
          You can customise this text in the Administration section.
        </p>
      </div>
    </section>
  );
}
